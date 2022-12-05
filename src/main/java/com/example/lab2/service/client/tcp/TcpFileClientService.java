package com.example.lab2.service.client.tcp;

import com.example.lab2.entity.FileMeta;
import com.example.lab2.entity.TransmissionPacket;
import com.example.lab2.entity.constants.CommandType;
import com.example.lab2.entity.constants.FileTransferStage;
import com.example.lab2.entity.constants.TimeOut;
import com.example.lab2.util.BitrateUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;

import static com.example.lab2.entity.Constants.BUFFER_SIZE;
import static com.example.lab2.util.TcpUtil.receivePacketWithTimeOutRetries;
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

        sendPacket(socket, new TransmissionPacket(
            CommandType.DOWNLOAD, FileTransferStage.GREETING,
            new FileMeta().setFileName(fileName)));
        var metaPacket = receivePacketWithTimeOutRetries(socket, TimeOut.DOWNLOAD);
        var canRead = metaPacket.getFileMeta().isCanRead();
        if (!canRead) {
            log.warn("File doesn't exists on server");
            return;
        }
        try (RandomAccessFile file = new RandomAccessFile(fileNameBuilder.toString(), "rwd")) {
            bitrateUtil.addTimeBorder(System.currentTimeMillis());

            long numberOfBlockToRequest = metaPacket.getFileMeta().getNumberOfLastReceivedBlock();
            while (true) {
                var fileMeta = new FileMeta()
                    .setFileName(fileName);

                sendPacket(socket, new TransmissionPacket(
                    CommandType.DOWNLOAD, FileTransferStage.DATA_REQUEST,
                    fileMeta.setNumberOfRequestingBlock(numberOfBlockToRequest++))
                );

                var data = receivePacketWithTimeOutRetries(socket, TimeOut.DOWNLOAD);
                if (data.getFileMeta().isEof()) {
                    bitrateUtil.setFileSize(data.getFileMeta().getFileSize());
                    break;
                }

                log.info("recieved packet " + data.getFileMeta().getNumberOfSendingBlock());
                file.seek(data.getFileMeta().getNumberOfSendingBlock() * BUFFER_SIZE);
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
            sendPacket(socket, new TransmissionPacket(
                CommandType.UPLOAD, FileTransferStage.GREETING,
                new FileMeta().setFileName(fileName).setCanRead(true)));

            byte[] buffer = new byte[BUFFER_SIZE];
            boolean isEOF;
            while (true) {
                var fileMeta = new FileMeta()
                    .setFileName(fileName)
                    .setCanRead(true)
                    .setFileSize(file.length())
                    .setEof(false);

                var requestPacket = receivePacketWithTimeOutRetries(socket, TimeOut.UPLOAD);
                long numberOfRequestingBlock = requestPacket.getFileMeta().getNumberOfRequestingBlock();
                fileInputStream.getChannel().position(numberOfRequestingBlock * BUFFER_SIZE);

                isEOF = fileInputStream.read(buffer) == -1;
                if (isEOF) {
                    sendPacket(socket, new TransmissionPacket(
                        CommandType.UPLOAD, FileTransferStage.INFO,
                        fileMeta.setEof(true))
                    );
                    break;
                }

                sendPacket(socket, new TransmissionPacket(
                    CommandType.UPLOAD, FileTransferStage.DATA_RESPONSE,
                    Arrays.copyOf(buffer, buffer.length), fileMeta.setNumberOfSendingBlock(numberOfRequestingBlock))
                );
            }

            System.out.println("File sent");
        } catch (FileNotFoundException e) {
            sendPacket(socket, new TransmissionPacket(
                CommandType.UPLOAD, FileTransferStage.META,
                new FileMeta().setFileName(fileName).setCanRead(file.canRead()))
            );
        }
    }

    public void printBitrate() {
        log.info(bitrateUtil.countBitRate() + "KByte/s");
        bitrateUtil.clearBorders();
    }
}
