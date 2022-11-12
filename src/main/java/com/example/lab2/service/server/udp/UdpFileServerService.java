package com.example.lab2.service.server.udp;

import com.example.lab2.entity.TransmissionPacket;
import com.example.lab2.entity.constants.CommandType;
import com.example.lab2.entity.constants.Offsets;
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
public class UdpFileServerService {
    private final BitrateUtil bitrateUtil;
    private final Offsets offsets;
    private final Map<Long, TransmissionPacket> sendingWindow;

    private String lastFileNameReceive = "";
    private String lastFileNameSend = "";

    public UdpFileServerService() {
        this.bitrateUtil = new BitrateUtil();
        this.sendingWindow = new HashMap<>();
        this.offsets = new Offsets(0, 0);
    }

    public void receiveFile(DatagramSocket socket, InetAddress senderAddress,
                            Integer senderPort, String fileName) throws IOException {
        StringBuilder fileNameBuilder = new StringBuilder("./");
        fileNameBuilder
                .append("server")
                .append("/")
                .append(fileName);
        if (!lastFileNameReceive.equals(fileName)) {
            offsets.setServerReceived(0);
        }
        lastFileNameReceive = fileName;

        var canReadOptional = receivePacketWithTimeOutAndSendAck(socket, TimeOut.UPLOAD);
        if (canReadOptional.isEmpty()) {
            log.error("Couldn't receive file can read");
            return;
        }
        boolean canRead = (boolean) convertBytesToObject(canReadOptional.get().getData());
        if (!canRead) {
            log.warn("File doesn't exists on client");
            return;
        }
        if (sendPacketAndReceiveAckWithTimeOut(socket, senderAddress, senderPort,
                new TransmissionPacket(CommandType.UPLOAD, convertObjectToBytes(offsets.getServerReceived())), TimeOut.UPLOAD).isEmpty()) {
            log.error("Couldn't send primary offset");
            return;
        }
        try (RandomAccessFile file = new RandomAccessFile(fileNameBuilder.toString(), "rwd")) {
            bitrateUtil.addTimeBorder(System.currentTimeMillis());
            while (true) {
                var dataOptional = receivePacketWithTimeOutAndSendAck(socket, TimeOut.UPLOAD);
                if (dataOptional.isEmpty()) {
                    log.warn("Couldn't receive file");
                    return;
                }
                var data = dataOptional.get();
                if (data.isEof()) {
                    bitrateUtil.setFileSize(data.getFileSize());
                    break;
                }
                log.info("recieved packet " + data.getNumberOfPacket());
                offsets.setServerReceived(data.getMinGuaranteedReceived() + 1);
                file.seek(data.getNumberOfPacket() * BUFFER_SIZE);
                file.write(data.getData());
            }

            offsets.setServerReceived(0);
            log.info("File received");
        } finally {
            bitrateUtil.addTimeBorder(System.currentTimeMillis());
        }
    }

    public void sendFile(DatagramSocket socket, InetAddress recipientAddress,
                         Integer recipientPort, String fileName) throws IOException {
        StringBuilder fileNameBuilder = new StringBuilder("./");
        fileNameBuilder
                .append("server")
                .append("/")
                .append(fileName);
        if (!lastFileNameSend.equals(fileName)) {
            offsets.setClientReceived(0);
        }
        lastFileNameSend = fileName;

        File file = new File(fileNameBuilder.toString());
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            if (sendPacketAndReceiveAckWithTimeOut(socket, recipientAddress, recipientPort,
                    new TransmissionPacket(CommandType.DOWNLOAD, convertObjectToBytes(file.canRead())), TimeOut.DOWNLOAD).isEmpty()) {
                log.error("Couldn't send file can read");
                return;
            }
            var fileSize = file.length();

            var primaryOffset = offsets.getClientReceived();

            long numberOfPacket = primaryOffset;
            fileInputStream.getChannel().position(primaryOffset * BUFFER_SIZE);
            byte[] buffer = new byte[BUFFER_SIZE];
            boolean isEOF = false;
            long minGuaranteed = 0;
            int manyPacketsNotReceivedTimes = 0;
            while (true) {
                if (isEOF && sendingWindow.isEmpty()) {
                    break;
                }
                while (sendingWindow.size() < 5) {
                    isEOF = fileInputStream.read(buffer) == -1;
                    if (isEOF) {
                        break;
                    }
                    var data = new TransmissionPacket(CommandType.DOWNLOAD, Arrays.copyOf(buffer, buffer.length),
                            numberOfPacket, fileName, fileSize, minGuaranteed);
                    sendingWindow.put(data.getNumberOfPacket(), data);
                    numberOfPacket++;
                }
                offsets.setClientReceived((long) sendingWindow.keySet().toArray()[0]);
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

            offsets.setClientReceived(0);
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
