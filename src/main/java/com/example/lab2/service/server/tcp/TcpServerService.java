package com.example.lab2.service.server.tcp;

import com.example.lab2.entity.TransmissionPacket;
import com.example.lab2.entity.constants.CommandType;
import com.example.lab2.entity.constants.TimeOut;
import com.example.lab2.service.pool.ClientSocketPool;
import com.example.lab2.service.server.TransferServerService;
import com.example.lab2.util.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.time.LocalTime;

import static com.example.lab2.util.TcpUtil.*;

@Slf4j
@Component
@Profile({"server & tcp"})
@RequiredArgsConstructor
public class TcpServerService implements TransferServerService {
    private final ClientSocketPool clientSocketPool;
    private final TcpFileServerService tcpFileServerService;

    @Override
    public void runServerHandler(ServerSocket serverSocket) throws IOException {
        while (true) {
            var clientSocketOptional = acceptClientWithTimeOut(serverSocket, TimeOut.CONNECTION);
            clientSocketOptional.ifPresent(clientSocket -> {
                    var clientUid = generateUid(clientSocket);
                    clientSocketPool.putClientSocket(clientUid, clientSocket);
                    log.info("Client connected: " + clientUid);
                }
            );

            clientSocketPool.getClientSocketUids().stream().forEach(clientSocketUid -> {
                var clientSocket = clientSocketPool.getClientSocket(clientSocketUid);
                runCommandHandler(clientSocket);
            });


        }
    }

    @Override
    public void runCommandHandler(Socket clientSocket) {
        try {
            var commandOptional = receiveCommandWithTimeOut(clientSocket, TimeOut.COMMAND);
            if (commandOptional.isPresent()) {
                var command = commandOptional.get();
                switch (command.getCommandType()) {
                    case ECHO -> sendEcho(clientSocket, command);
                    case TIME -> sendTime(clientSocket, command);
                    case UPLOAD -> receiveFile(clientSocket, command);
                    case DOWNLOAD -> sendFile(clientSocket, command);
                }
            }
        } catch (SocketException | SocketTimeoutException e) {
            log.info("Client disconnected");
        } catch (Exception e) {
            log.info("Client disconnected");
            e.printStackTrace();
        }
    }

    @Override
    public void sendEcho(Socket clientSocket, TransmissionPacket receivedCommand) throws IOException {
        log.info("Echo: " + new String(receivedCommand.getData()));
        sendPacket(clientSocket, new TransmissionPacket(CommandType.ECHO, receivedCommand.getData(), true));
    }

    @Override
    public void sendTime(Socket clientSocket, TransmissionPacket receivedCommand) throws IOException {
        var time = LocalTime.now();
        log.info("Time: " + time.toString());
        sendPacket(clientSocket, new TransmissionPacket(CommandType.TIME, Converter.convertObjectToBytes(time), true));
    }

    @Override
    public void receiveFile(Socket clientSocket, TransmissionPacket receivedCommand) throws IOException {
        log.info("UPLOAD: " + receivedCommand.getFileMeta().getFileName());
        tcpFileServerService.receiveFile(clientSocket, receivedCommand.getFileMeta().getFileName(), receivedCommand);
    }

    @Override
    public void sendFile(Socket clientSocket, TransmissionPacket receivedCommand) throws IOException {
        log.info("DOWNLOAD: " + receivedCommand.getFileMeta().getFileName());
        tcpFileServerService.sendFile(clientSocket, receivedCommand.getFileMeta().getFileName(), receivedCommand);
    }
}
