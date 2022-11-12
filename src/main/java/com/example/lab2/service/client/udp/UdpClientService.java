package com.example.lab2.service.client.udp;

import com.example.lab2.entity.SocketMeta;
import com.example.lab2.entity.TransmissionPacket;
import com.example.lab2.entity.constants.CommandType;
import com.example.lab2.entity.constants.TimeOut;
import com.example.lab2.service.client.TransferClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.time.LocalTime;

import static com.example.lab2.util.Converter.convertBytesToObject;
import static com.example.lab2.util.Converter.convertObjectToBytes;
import static com.example.lab2.util.UdpUtil.sendPacketAndReceiveAckWithTimeOut;

@Primary
@Slf4j
@Component
@Profile({"client & udp"})
public class UdpClientService implements TransferClientService {
    private DatagramSocket clientSocket;
    private SocketMeta serverMeta;
    private UdpFileClientService udpFileClientService;

    @Autowired
    public UdpClientService() throws SocketException {
        this.clientSocket = new DatagramSocket();
        this.serverMeta = new SocketMeta();
        this.udpFileClientService = new UdpFileClientService();
    }

    @Override
    public void connectServer(String host, int port) throws IOException {
        serverMeta = new SocketMeta();
        serverMeta.setAddress(InetAddress.getByName(host));
        serverMeta.setPort(port);
        var connectRequest = new TransmissionPacket(CommandType.CONNECT);
        sendPacketAndReceiveAckWithTimeOut(
                clientSocket, serverMeta.getAddress(), serverMeta.getPort(), connectRequest, TimeOut.CONNECTION);
    }

    @Override
    public void disconnectServer() {
        clientSocket.close();
    }

    @Override
    public String sendEcho(String echoString) throws IOException {
        var packetToSend = new TransmissionPacket(CommandType.ECHO, convertObjectToBytes(echoString));
        var receivedPacket = sendPacketAndReceiveAckWithTimeOut(
                clientSocket, serverMeta.getAddress(), serverMeta.getPort(), packetToSend, TimeOut.ECHO);
        return receivedPacket
                .map(packet -> (String) convertBytesToObject(receivedPacket.get().getData()))
                .orElse("Couldn't execute echo command");
    }

    @Override
    public String requestTime() throws IOException {
        var packetToSend = new TransmissionPacket(CommandType.TIME);
        var receivedPacket = sendPacketAndReceiveAckWithTimeOut(
                clientSocket, serverMeta.getAddress(), serverMeta.getPort(), packetToSend, TimeOut.TIME);
        return receivedPacket
                .map(packet -> ((LocalTime) convertBytesToObject(packet.getData())).toString())
                .orElse("Couldn't execute time command");
    }

    @Override
    public void uploadFile(String filename) throws IOException {
        if (sendPacketAndReceiveAckWithTimeOut(clientSocket, serverMeta.getAddress(),
                serverMeta.getPort(), new TransmissionPacket(CommandType.UPLOAD, filename), TimeOut.UPLOAD)
                .isPresent()) {
            udpFileClientService.sendFile(clientSocket, serverMeta.getAddress(), serverMeta.getPort(), filename);
        } else {
            log.error("Couldn't initiate upload file");
        }
    }

    @Override
    public void downloadFile(String fileName) throws IOException {
        if (sendPacketAndReceiveAckWithTimeOut(clientSocket, serverMeta.getAddress(),
                serverMeta.getPort(), new TransmissionPacket(CommandType.DOWNLOAD, fileName), TimeOut.DOWNLOAD)
                .isPresent()) {
            udpFileClientService.receiveFile(clientSocket, serverMeta.getAddress(), serverMeta.getPort(), fileName);
            udpFileClientService.printBitrate();
        } else {
            log.error("Couldn't initiate download file");
        }
    }
}
