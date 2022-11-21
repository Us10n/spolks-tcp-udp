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

        var packet = receivePacketWithTimeOut(socket, TimeOut.DOWNLOAD);
        var canRead = (boolean) convertBytesToObject(packet.getData());
        if (!canRead) {
            log.warn("File doesn't exists on server");
            return;
        }

        try (RandomAccessFile file = new RandomAccessFile(fileNameBuilder.toString(), "rwd")) {
            bitrateUtil.addTimeBorder(System.currentTimeMillis());
            while (true) {
                var data = receivePacketWithTimeOut(socket, TimeOut.DOWNLOAD);
                sendPacket(socket, new TransmissionPacket(CommandType.DOWNLOAD, true));
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

            var packet = receivePacketWithTimeOut(socket, TimeOut.UPLOAD);
            long primaryOffset = (Long) convertBytesToObject(packet.getData());
            long numberOfPacket = primaryOffset;
            fileInputStream.getChannel().position(primaryOffset * BUFFER_SIZE);
            byte[] buffer = new byte[BUFFER_SIZE];
            boolean isEOF = false;
            long minGuaranteed = 0;

            while (true) {
                isEOF = fileInputStream.read(buffer) == -1;
                if (isEOF) {
                    sendPacket(socket, new TransmissionPacket(CommandType.UPLOAD).setEof(true).setFileSize(file.length()));
                    receivePacketWithTimeOut(socket, TimeOut.UPLOAD);
                    break;
                }
                var data = new TransmissionPacket(CommandType.UPLOAD, Arrays.copyOf(buffer, buffer.length),
                        numberOfPacket, fileName, fileSize, minGuaranteed);
                numberOfPacket++;

                sendPacket(socket, data);
                receivePacketWithTimeOut(socket, TimeOut.UPLOAD);
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
