package com.example.lab2.service.server.tcp;

import com.example.lab2.entity.FileMeta;
import com.example.lab2.entity.TransmissionPacket;
import com.example.lab2.entity.constants.CommandType;
import com.example.lab2.entity.constants.FileTransferStage;
import com.example.lab2.entity.constants.Offsets;
import com.example.lab2.entity.constants.TimeOut;
import com.example.lab2.util.BitrateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.example.lab2.entity.Constants.BUFFER_SIZE;
import static com.example.lab2.util.TcpUtil.*;
import static java.util.Objects.isNull;

@Slf4j
@Component
public class TcpFileServerService {
    private Map<String, BitrateUtil> bitrateUtilMap = new HashMap<>();
    private Map<String, Offsets> offsetsMap = new HashMap<>();

    public void receiveFile(Socket socket, String fileName, TransmissionPacket packet) throws IOException {
        StringBuilder fileNameBuilder = new StringBuilder("./");
        fileNameBuilder
            .append("server")
            .append("/")
            .append(fileName);
        var clientUid = generateUid(socket);

        if (isNull(offsetsMap.get(clientUid))) {
            offsetsMap.put(clientUid, new Offsets());
        }
        if (isNull(bitrateUtilMap.get(clientUid))) {
            bitrateUtilMap.put(clientUid, new BitrateUtil());
        }
        var offsets = offsetsMap.get(clientUid);
        var bitrateUtil = bitrateUtilMap.get(clientUid);
        if (!offsets.getLastFileServerReceived().equals(fileName)) {
            offsets.setServerReceived(0L);
        }
        offsets.setLastFileServerReceived(fileName);

        boolean canRead = packet.getFileMeta().isCanRead();
        if (!canRead) {
            log.warn("File doesn't exists on client");
            return;
        }

        if (packet.getFileTransferStage().equals(FileTransferStage.GREETING) && packet.getFileMeta().isCanRead()) {
            sendPacket(socket, new TransmissionPacket(
                CommandType.UPLOAD, FileTransferStage.DATA_REQUEST,
                packet.getFileMeta().setNumberOfRequestingBlock(offsets.getServerReceived()))
            );
            return;
        }
        if (packet.getFileTransferStage().equals(FileTransferStage.INFO) && packet.getFileMeta().isEof()) {
            bitrateUtil.setFileSize(packet.getFileMeta().getFileSize());
            offsets.setServerReceived(0L);
            log.info("File received");
            printBitrate(clientUid);
            return;
        }
        if (packet.getFileTransferStage().equals(FileTransferStage.DATA_RESPONSE)) {
            bitrateUtil.addTimeBorder(System.currentTimeMillis());
            var numberOfSendingBlock = packet.getFileMeta().getNumberOfSendingBlock();
            try (RandomAccessFile file = new RandomAccessFile(fileNameBuilder.toString(), "rwd")) {
                file.seek(numberOfSendingBlock * BUFFER_SIZE);
                file.write(packet.getData());
            }
            log.info("received packet " + numberOfSendingBlock);
            offsets.setServerReceived(numberOfSendingBlock + 1);
            sendPacket(socket, new TransmissionPacket(
                CommandType.UPLOAD, FileTransferStage.DATA_REQUEST,
                packet.getFileMeta().setNumberOfRequestingBlock(offsets.getServerReceived()))
            );
            bitrateUtil.addTimeBorder(System.currentTimeMillis());
        }
    }

    public void sendFile(Socket socket, String fileName, TransmissionPacket packet) throws IOException {
        StringBuilder fileNameBuilder = new StringBuilder("./");
        fileNameBuilder
            .append("server")
            .append("/")
            .append(fileName);
        var clientUid = generateUid(socket);

        if (isNull(offsetsMap.get(clientUid))) {
            offsetsMap.put(clientUid, new Offsets());
        }
        var offsets = offsetsMap.get(clientUid);
        if (!offsets.getLastFileClientReceived().equals(fileName)) {
            offsets.setClientReceived(0L);
        }
        offsets.setLastFileClientReceived(fileName);

        File file = new File(fileNameBuilder.toString());
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            var fileMeta = new FileMeta()
                .setFileName(fileName)
                .setCanRead(true)
                .setFileSize(file.length())
                .setEof(false);

            if (packet.getFileTransferStage().equals(FileTransferStage.GREETING)) {
                sendPacket(socket, new TransmissionPacket(
                    CommandType.DOWNLOAD, FileTransferStage.META,
                    new FileMeta().setFileName(fileName).setCanRead(true).setNumberOfLastReceivedBlock(offsets.getClientReceived())));
            }
            if (packet.getFileTransferStage().equals(FileTransferStage.DATA_REQUEST)) {
                long numberOfRequestingBlock = packet.getFileMeta().getNumberOfRequestingBlock();
                offsets.setClientReceived(numberOfRequestingBlock);

                fileInputStream.getChannel().position(numberOfRequestingBlock * BUFFER_SIZE);
                byte[] buffer = new byte[BUFFER_SIZE];
                boolean isEOF;

                isEOF = fileInputStream.read(buffer) == -1;
                if (isEOF) {
                    sendPacket(socket, new TransmissionPacket(
                        CommandType.DOWNLOAD, FileTransferStage.INFO,
                        fileMeta.setEof(true))
                    );
                    offsets.setClientReceived(0L);
                    log.info("File sent");
                } else {
//                    log.info("Sent block " + numberOfRequestingBlock);
                    sendPacket(socket, new TransmissionPacket(
                        CommandType.DOWNLOAD, FileTransferStage.DATA_RESPONSE,
                        Arrays.copyOf(buffer, buffer.length), fileMeta.setNumberOfSendingBlock(numberOfRequestingBlock))
                    );
                }
            }

        } catch (FileNotFoundException e) {
            if (packet.getFileTransferStage().equals(FileTransferStage.GREETING)) {
                sendPacket(socket, new TransmissionPacket(
                    CommandType.DOWNLOAD, FileTransferStage.GREETING,
                    new FileMeta().setFileName(fileName).setCanRead(true)));
            }
        }
    }

    public void printBitrate(String clientUid) {
        var bitrateUtil = bitrateUtilMap.get(clientUid);
        log.info(bitrateUtil.countBitRate() + "KByte/s");
        bitrateUtil.clearBorders();
    }
}

