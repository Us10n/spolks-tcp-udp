package com.example.lab2.service.client.tcp;

import com.example.lab2.entity.TransmissionPacket;
import com.example.lab2.entity.constants.CommandType;
import com.example.lab2.entity.constants.TimeOut;
import com.example.lab2.util.BitrateUtil;
import com.example.lab2.util.Converter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;

import static com.example.lab2.entity.constants.Constants.BUFFER_SIZE;
import static com.example.lab2.util.Converter.convertBytesToObject;
import static com.example.lab2.util.TcpUtil.receivePacketWithTimeOut;
import static com.example.lab2.util.TcpUtil.sendPacket;

@Slf4j
public class TcpFileClientService {
    private BitrateUtil bitrateUtil;

    public TcpFileClientService() {
        this.bitrateUtil = new BitrateUtil();
    }

    public void receiveFile(Socket socket, String fileName) throws IOException {
        StringBuilder fileNameBuilder = new StringBuilder("./");
        fileNameBuilder
                .append("client")
                .append("/")
                .append(fileName);

        var canReadOptional = receivePacketWithTimeOut(socket, TimeOut.DOWNLOAD);
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
                var dataOptional = receivePacketWithTimeOut(socket, TimeOut.DOWNLOAD);
                sendPacket(socket, new TransmissionPacket(CommandType.DOWNLOAD, true));
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

            System.out.println("File received");
        } finally {
            bitrateUtil.addTimeBorder(System.currentTimeMillis());
        }
    }

    public void sendFile(Socket socket, String fileName) throws IOException {
        StringBuilder fileNameBuilder = new StringBuilder("./");
        fileNameBuilder
                .append("client")
                .append("/")
                .append(fileName);

        File file = new File(fileNameBuilder.toString());
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            sendPacket(socket, new TransmissionPacket(CommandType.UPLOAD, Converter.convertObjectToBytes(file.canRead())));
            var fileSize = file.length();

            var primaryOffsetOptional = receivePacketWithTimeOut(socket, TimeOut.UPLOAD);
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

            while (true) {
                isEOF = fileInputStream.read(buffer) == -1;
                if (isEOF) {
                    sendPacket(socket, new TransmissionPacket(CommandType.UPLOAD).setEof(true).setFileSize(file.length()));
                    break;
                }
                var data = new TransmissionPacket(CommandType.UPLOAD, Arrays.copyOf(buffer, buffer.length),
                        numberOfPacket, fileName, fileSize, minGuaranteed);
                numberOfPacket++;

                sendPacket(socket, data);
                if (receivePacketWithTimeOut(socket, TimeOut.UPLOAD).isEmpty()) {
                    log.info("Server doesn't receive many packet");
                    return;
                }
            }

            System.out.println("File sent");
        } catch (FileNotFoundException e) {
            sendPacket(socket, new TransmissionPacket(CommandType.UPLOAD, Converter.convertObjectToBytes(file.canRead())));
        }
    }

    public void printBitrate() {
        log.info(bitrateUtil.countBitRate() + "KByte/s");
        bitrateUtil.clearBorders();
    }
}
