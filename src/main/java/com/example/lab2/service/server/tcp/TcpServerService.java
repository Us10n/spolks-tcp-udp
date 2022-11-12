package com.example.lab2.service.server.tcp;

import com.example.lab2.entity.TransmissionPacket;
import com.example.lab2.entity.constants.CommandType;
import com.example.lab2.service.server.TransferServerService;
import com.example.lab2.util.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;

import static com.example.lab2.util.TcpUtil.receivePacket;
import static com.example.lab2.util.TcpUtil.sendPacket;

@Slf4j
@Component
@Profile({"server & tcp"})
public class TcpServerService implements TransferServerService {
    private final static int SERVER_PORT = 8888;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private TcpFileServerService tcpFileServerService;
    private boolean isClientConnected = false;

    public TcpServerService() throws IOException {
        this.serverSocket = new ServerSocket(SERVER_PORT);
        this.tcpFileServerService = new TcpFileServerService();
    }

    @Override
    public void runHandler() throws IOException {
        while (true) {
            this.clientSocket = serverSocket.accept();
            log.info("Client connected: " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());
            try {
                while (true) {
                    var command = readCommand();
                    switch (command.getCommandType()) {
                        case CONNECT -> connect(command);
                        case ECHO -> sendEcho(command);
                        case TIME -> sendTime(command);
                        case UPLOAD -> receiveFile(command);
                        case DOWNLOAD -> sendFile(command);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public TransmissionPacket readCommand() throws IOException {
        return receivePacket(clientSocket);
    }

    @Override
    public void sendEcho(TransmissionPacket receivedCommand) throws IOException {
        log.info("Echo: " + new String(receivedCommand.getData()));
        sendPacket(clientSocket, new TransmissionPacket(CommandType.ECHO, receivedCommand.getData(), true));
    }

    @Override
    public void sendTime(TransmissionPacket receivedCommand) throws IOException {
        var time = LocalTime.now();
        log.info("Time: " + time.toString());
        sendPacket(clientSocket, new TransmissionPacket(CommandType.ECHO, Converter.convertObjectToBytes(time), true));
    }

    @Override
    public void connect(TransmissionPacket receivedCommand) throws IOException {
    }

    @Override
    public void receiveFile(TransmissionPacket receivedCommand) throws IOException {
        log.info("UPLOAD: " + receivedCommand.getFileName());
        tcpFileServerService.receiveFile(clientSocket, receivedCommand.getFileName());
        tcpFileServerService.printBitrate();
    }

    @Override
    public void sendFile(TransmissionPacket receivedCommand) throws IOException {
        log.info("DOWNLOAD: " + receivedCommand.getFileName());
        tcpFileServerService.sendFile(clientSocket, receivedCommand.getFileName());
    }
}
