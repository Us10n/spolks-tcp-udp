package com.example.lab2.service.server.udp;

import com.example.lab2.entity.SocketMeta;
import com.example.lab2.entity.TransmissionPacket;
import com.example.lab2.entity.constants.CommandType;
import com.example.lab2.service.server.TransferServerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.time.LocalTime;

import static com.example.lab2.util.Converter.convertBytesToTransmissionPacket;
import static com.example.lab2.util.Converter.convertObjectToBytes;
import static com.example.lab2.util.UdpUtil.receiveDatagram;
import static com.example.lab2.util.UdpUtil.sendObject;

@Primary
@Slf4j
@Component
@Profile({"server & udp"})
public class UdpServerService implements TransferServerService {
    private final static int SERVER_PORT = 8888;

    private DatagramSocket serverSocket;
    private SocketMeta clientMeta;
    private UdpFileServerService udpFileServerService;

    public UdpServerService() throws SocketException {
        this.serverSocket = new DatagramSocket(SERVER_PORT);
        this.udpFileServerService = new UdpFileServerService();
    }

    @Override
    public void runHandler() {
        while (true) {
            try {
                var command = readCommand();
                switch (command.getCommandType()) {
                    case CONNECT -> connect(command);
                    case ECHO -> sendEcho(command);
                    case TIME -> sendTime(command);
                    case UPLOAD -> receiveFile(command);
                    case DOWNLOAD -> sendFile(command);
                }
            } catch (SocketException | SocketTimeoutException e) {
                log.info("Client disconnected");
            } catch (Exception e) {
                log.error("Unknown exception");
                e.printStackTrace();
            }
        }
    }

    @Override
    public TransmissionPacket readCommand() throws IOException {
        var receivingPacket = receiveDatagram(serverSocket);

        clientMeta = clientMeta == null ? new SocketMeta() : clientMeta;
        clientMeta.setAddress(receivingPacket.getAddress());
        clientMeta.setPort(receivingPacket.getPort());
        return convertBytesToTransmissionPacket(receivingPacket.getData());
    }

    @Override
    public void sendEcho(TransmissionPacket receivedCommand) throws IOException {
        log.info("Echo: " + new String(receivedCommand.getData()));
        var response = new TransmissionPacket(CommandType.ECHO, receivedCommand.getData(), true);
        sendObject(serverSocket, clientMeta.getAddress(), clientMeta.getPort(), response);
    }

    @Override
    public void sendTime(TransmissionPacket receivedCommand) throws IOException {
        var time = LocalTime.now();
        log.info("Time: " + time.toString());
        var response = new TransmissionPacket(CommandType.TIME, convertObjectToBytes(time), true);
        sendObject(serverSocket, clientMeta.getAddress(), clientMeta.getPort(), response);
    }

    @Override
    public void connect(TransmissionPacket receivedCommand) throws IOException {
        var response = new TransmissionPacket(CommandType.CONNECT, true);
        sendObject(serverSocket, clientMeta.getAddress(), clientMeta.getPort(), response);
        log.info("Client connected: " + clientMeta);
    }

    @Override
    public void receiveFile(TransmissionPacket receivedCommand) throws IOException {
        log.info("UPLOAD: " + receivedCommand.getFileName());
        sendObject(serverSocket, clientMeta.getAddress(), clientMeta.getPort(), new TransmissionPacket(CommandType.UPLOAD, true));
        udpFileServerService.receiveFile(serverSocket, clientMeta.getAddress(), clientMeta.getPort(), receivedCommand.getFileName());
        udpFileServerService.printBitrate();
    }

    @Override
    public void sendFile(TransmissionPacket receivedCommand) throws IOException {
        log.info("DOWNLOAD: " + receivedCommand.getFileName());
        sendObject(serverSocket, clientMeta.getAddress(), clientMeta.getPort(), new TransmissionPacket(CommandType.DOWNLOAD, true));
        udpFileServerService.sendFile(serverSocket, clientMeta.getAddress(), clientMeta.getPort(), receivedCommand.getFileName());
    }
}
