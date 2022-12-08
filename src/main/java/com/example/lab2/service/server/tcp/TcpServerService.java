package com.example.lab2.service.server.tcp;

import com.example.lab2.entity.TransmissionPacket;
import com.example.lab2.entity.constants.CommandType;
import com.example.lab2.util.Converter;
import com.example.lab2.util.TcpUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.time.LocalTime;

import static com.example.lab2.util.TcpUtil.receivePacket;
import static com.example.lab2.util.TcpUtil.sendPacket;

@Slf4j
public class TcpServerService implements Runnable {
    private Socket clientSocket;
    private TcpFileServerService tcpFileServerService;

    public TcpServerService(Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        this.tcpFileServerService = new TcpFileServerService();
    }

    @SneakyThrows
    @Override
    public void run() {
        runHandler();
    }


    public void runHandler() throws IOException {
        try {
            while (true) {
                var command = readCommand();
//                log.info(command.toString());
                switch (command.getCommandType()) {
                    case CONNECT -> connect(command);
                    case ECHO -> sendEcho(command);
                    case TIME -> sendTime(command);
                    case UPLOAD -> receiveFile(command);
                    case DOWNLOAD -> sendFile(command);
                }
            }
        } catch (EOFException | SocketException | SocketTimeoutException e) {
            log.info("Client disconnected: " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());
            clientSocket.close();
        } catch (Exception e) {
            log.info("Client disconnected: " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());
            e.printStackTrace();
        }
    }


    public TransmissionPacket readCommand() throws IOException {
        return receivePacket(clientSocket);
    }


    public void sendEcho(TransmissionPacket receivedCommand) throws IOException {
        log.info("Echo: " + new String(receivedCommand.getData()));
        sendPacket(clientSocket, new TransmissionPacket(CommandType.ECHO, receivedCommand.getData(), true));
    }


    public void sendTime(TransmissionPacket receivedCommand) throws IOException {
        var time = LocalTime.now();
        log.info("Time: " + time.toString());
        sendPacket(clientSocket, new TransmissionPacket(CommandType.TIME, Converter.convertObjectToBytes(time), true));
    }


    public void connect(TransmissionPacket receivedCommand) throws IOException {
    }

    public void receiveFile(TransmissionPacket receivedCommand) throws IOException {
        log.info("UPLOAD: " + receivedCommand.getFileName());
        tcpFileServerService.receiveFile(clientSocket, receivedCommand.getFileName());
        tcpFileServerService.printBitrate();
    }


    public void sendFile(TransmissionPacket receivedCommand) throws IOException {
        log.info("DOWNLOAD: " + receivedCommand.getFileName());
        tcpFileServerService.sendFile(clientSocket, receivedCommand.getFileName());
    }
}
