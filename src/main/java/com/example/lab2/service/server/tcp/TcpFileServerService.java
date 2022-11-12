package com.example.lab2.service.server.tcp;

import com.example.lab2.entity.TransmissionPacket;
import com.example.lab2.entity.constants.CommandType;
import com.example.lab2.entity.constants.Offsets;
import com.example.lab2.entity.constants.TimeOut;
import com.example.lab2.util.BitrateUtil;
import com.example.lab2.util.Converter;
import com.example.lab2.util.TcpUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;

import static com.example.lab2.entity.constants.Constants.BUFFER_SIZE;
import static com.example.lab2.util.Converter.convertBytesToObject;
import static com.example.lab2.util.Converter.convertObjectToBytes;
import static com.example.lab2.util.TcpUtil.receivePacketWithTimeOut;
import static com.example.lab2.util.TcpUtil.sendPacket;

@Slf4j
public class TcpFileServerService {
    private final BitrateUtil bitrateUtil;
    private final Offsets offsets;

    private String lastFileNameReceive = "";
    private String lastFileNameSend = "";

    public TcpFileServerService() {
        this.bitrateUtil = new BitrateUtil();
        this.offsets = new Offsets(0, 0);
    }

    public void receiveFile(Socket socket, String fileName) throws IOException {
        StringBuilder fileNameBuilder = new StringBuilder("./");
        fileNameBuilder
                .append("server")
                .append("/")
                .append(fileName);
        if (!lastFileNameReceive.equals(fileName)) {
            offsets.setServerReceived(0);
        }
        lastFileNameReceive = fileName;

        var canReadOptional = TcpUtil.receivePacketWithTimeOut(socket, TimeOut.UPLOAD);
        if (canReadOptional.isEmpty()) {
            log.error("Couldn't receive file can read");
            return;
        }
        boolean canRead = (boolean) convertBytesToObject(canReadOptional.get().getData());
        if (!canRead) {
            log.warn("File doesn't exists on client");
            return;
        }
        sendPacket(socket, new TransmissionPacket(CommandType.UPLOAD, convertObjectToBytes(offsets.getServerReceived())));

        try (RandomAccessFile file = new RandomAccessFile(fileNameBuilder.toString(), "rwd")) {
            bitrateUtil.addTimeBorder(System.currentTimeMillis());
            while (true) {
                var dataOptional = receivePacketWithTimeOut(socket, TimeOut.UPLOAD);
                if (dataOptional.isEmpty()) {
                    log.warn("Couldn't receive file");
                    return;
                }
                sendPacket(socket, new TransmissionPacket(CommandType.UPLOAD, true));
                var data = dataOptional.get();
                if (data.isEof()) {
                    bitrateUtil.setFileSize(data.getFileSize());
                    break;
                }
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

    public void sendFile(Socket socket, String fileName) throws IOException {
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
            sendPacket(socket, new TransmissionPacket(CommandType.DOWNLOAD, convertObjectToBytes(file.canRead())));
            var fileSize = file.length();

            var primaryOffset = offsets.getClientReceived();

            long numberOfPacket = primaryOffset;
            fileInputStream.getChannel().position(primaryOffset * BUFFER_SIZE);
            byte[] buffer = new byte[BUFFER_SIZE];
            boolean isEOF = false;
            long minGuaranteed = 0;

            while (!isEOF) {
                isEOF = fileInputStream.read(buffer) == -1;
                var data = new TransmissionPacket(CommandType.UPLOAD, Arrays.copyOf(buffer, buffer.length),
                        numberOfPacket, fileName, fileSize, minGuaranteed);
                numberOfPacket++;
                sendPacket(socket, data);
                if (receivePacketWithTimeOut(socket, TimeOut.UPLOAD).isEmpty()) {
                    log.info("Server can't send file");
                    return;
                }
            }
            sendPacket(socket, new TransmissionPacket(CommandType.UPLOAD).setEof(true).setFileSize(file.length()));

            offsets.setClientReceived(0);
            log.info("File sent");
        } catch (FileNotFoundException e) {
            sendPacket(socket, new TransmissionPacket(CommandType.UPLOAD, Converter.convertObjectToBytes(file.canRead())));
        }
    }

    public void printBitrate() {
        log.info(bitrateUtil.countBitRate() + "KByte/s");
        bitrateUtil.clearBorders();
    }
}

