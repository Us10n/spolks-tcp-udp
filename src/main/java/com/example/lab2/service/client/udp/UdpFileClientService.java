package com.example.lab2.service.client.udp;

import com.example.lab2.entity.TransmissionPacket;
import com.example.lab2.entity.constants.CommandType;
import com.example.lab2.entity.constants.TimeOut;
import com.example.lab2.util.BitrateUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.example.lab2.entity.constants.Constants.BUFFER_SIZE;
import static com.example.lab2.util.Converter.convertBytesToObject;
import static com.example.lab2.util.Converter.convertObjectToBytes;
import static com.example.lab2.util.UdpUtil.*;

@Slf4j
public class UdpFileClientService {
    private final BitrateUtil bitrateUtil;
    private final Map<Long, TransmissionPacket> sendingWindow;

    public UdpFileClientService() {
        this.bitrateUtil = new BitrateUtil();
        this.sendingWindow = new HashMap<>();
    }

    public void receiveFile(DatagramSocket socket, InetAddress senderAddress,
                            Integer senderPort, String fileName) throws IOException {
        StringBuilder fileNameBuilder = new StringBuilder("./");
        fileNameBuilder
                .append("client")
                .append("/")
                .append(fileName);

        var canReadOptional = receivePacketWithTimeOutAndSendAck(socket, TimeOut.DOWNLOAD);
        if (canReadOptional.isEmpty()) {
            log.error("Couldn't receive can read");
            return;
        }
        var canRead = (boolean) convertBytesToObject(canReadOptional.get().getData());
        if (!canRead) {
            log.warn("File doesn't exists on server");
            return;
        }

        try (RandomAccessFile file = new RandomAccessFile(fileNameBuilder.toString(), "rwd")) {
            bitrateUtil.addTimeBorder(System.currentTimeMillis());
            while (true) {
                var dataOptional = receivePacketWithTimeOutAndSendAck(socket, TimeOut.DOWNLOAD);
                if (dataOptional.isEmpty()) {
                    log.warn("Couldn't receive file");
                    return;
                }
                var data = dataOptional.get();
                if (data.isEof()) {
                    bitrateUtil.setFileSize(data.getFileSize());
                    break;
                }
//                log.info("recieved packet " + data.getNumberOfPacket());
                file.seek(data.getNumberOfPacket() * BUFFER_SIZE);
                file.write(data.getData());
            }

            log.info("File received");
        } finally {
            bitrateUtil.addTimeBorder(System.currentTimeMillis());
        }
    }

    public void sendFile(DatagramSocket socket, InetAddress recipientAddress,
                         Integer recipientPort, String fileName) throws IOException {
        StringBuilder fileNameBuilder = new StringBuilder("./");
        fileNameBuilder
                .append("client")
                .append("/")
                .append(fileName);

        File file = new File(fileNameBuilder.toString());
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            if (sendPacketAndReceiveAckWithTimeOut(socket, recipientAddress, recipientPort,
                    new TransmissionPacket(CommandType.UPLOAD, convertObjectToBytes(file.canRead())), TimeOut.UPLOAD).isEmpty()) {
                log.error("Couldn't send file can read");
                return;
            }
            var fileSize = file.length();

            var primaryOffsetOptional = receivePacketWithTimeOutAndSendAck(socket, 0);
            if (primaryOffsetOptional.isEmpty()) {
                log.error("Couldn't receive primary offset");
                return;
            }
            long primaryOffset = (Long) convertBytesToObject(primaryOffsetOptional.get().getData());
            long numberOfPacket = primaryOffset;
            fileInputStream.getChannel().position(primaryOffset * BUFFER_SIZE);
            byte[] buffer = new byte[BUFFER_SIZE];
            boolean isEOF = false;
            long minGuaranteed = 0;
            int manyPacketsNotReceivedTimes = 0;
            while (!isEOF && sendingWindow.isEmpty()) {
                while (sendingWindow.size() < 5) {
                    isEOF = fileInputStream.read(buffer) == -1;
                    var data = new TransmissionPacket(CommandType.UPLOAD, Arrays.copyOf(buffer, buffer.length),
                            numberOfPacket, fileName, fileSize, minGuaranteed);
                    sendingWindow.put(data.getNumberOfPacket(), data);
                    numberOfPacket++;
                }
                minGuaranteed = (long) sendingWindow.keySet().toArray()[0];
                for (TransmissionPacket packet : sendingWindow.values()) {
                    sendObject(socket, recipientAddress, recipientPort, packet);
                }
                Optional<TransmissionPacket> receivedPacket;
                while (!sendingWindow.isEmpty() && (receivedPacket = receivePacketWithTimeOut(socket, 0)).isPresent()) {
                    sendingWindow.remove(receivedPacket.get().getNumberOfPacket());
                }
                if (sendingWindow.size() > 3) {
                    manyPacketsNotReceivedTimes++;
                }
                if (manyPacketsNotReceivedTimes >= 5) {
                    log.info("Server doesn't receive many packet");
                    return;
                }
            }
            if (sendPacketAndReceiveAckWithTimeOut(socket, recipientAddress, recipientPort,
                    new TransmissionPacket(CommandType.UPLOAD).setEof(true).setFileSize(file.length()), TimeOut.UPLOAD).isEmpty()) {
                log.error("Couldn't send EOF");
                return;
            }

            log.info("File sent");
        } catch (FileNotFoundException e) {
            sendPacketAndReceiveAckWithTimeOut(socket, recipientAddress,
                    recipientPort, new TransmissionPacket(CommandType.UPLOAD, convertObjectToBytes(file.canRead())), TimeOut.UPLOAD);
        }
    }

    public void printBitrate() {
        log.info(bitrateUtil.countBitRate() + "KByte/s");
        bitrateUtil.clearBorders();
    }
}
