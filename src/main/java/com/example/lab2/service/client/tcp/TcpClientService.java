package com.example.lab2.service.client.tcp;

import com.example.lab2.entity.TransmissionPacket;
import com.example.lab2.entity.constants.CommandType;
import com.example.lab2.entity.constants.TimeOut;
import com.example.lab2.service.client.TransferClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalTime;

import static com.example.lab2.util.Converter.convertBytesToObject;
import static com.example.lab2.util.Converter.convertObjectToBytes;
import static com.example.lab2.util.TcpUtil.receivePacketWithTimeOut;
import static com.example.lab2.util.TcpUtil.sendPacket;

@Slf4j
@Component
@Profile({"client & tcp"})
public class TcpClientService implements TransferClientService {

    private Socket clientSocket;
    private TcpFileClientService tcpFileClientService;

    public TcpClientService() {
        this.tcpFileClientService = new TcpFileClientService();
    }

    @Override
    public void connectServer(String host, int port) throws IOException {
        clientSocket = new Socket();
        clientSocket.connect(new InetSocketAddress(host, port), TimeOut.CONNECTION);
    }

    @Override
    public void disconnectServer() throws IOException {
        clientSocket.close();
    }

    @Override
    public String sendEcho(String echoMessage) throws IOException {
        sendPacket(clientSocket, new TransmissionPacket(CommandType.ECHO, convertObjectToBytes(echoMessage)));
        var receivedPacket = receivePacketWithTimeOut(clientSocket, TimeOut.ECHO);
        return (String) convertBytesToObject(receivedPacket.getData());
    }

    @Override
    public String requestTime() throws IOException {
        sendPacket(clientSocket, new TransmissionPacket(CommandType.TIME));
        var receivePacket = receivePacketWithTimeOut(clientSocket, TimeOut.TIME);
        return ((LocalTime) convertBytesToObject(receivePacket.getData())).toString();
    }

    @Override
    public void uploadFile(String fileName) throws IOException {
        sendPacket(clientSocket, new TransmissionPacket(CommandType.UPLOAD, fileName));
        tcpFileClientService.sendFile(clientSocket, fileName);
    }

    @Override
    public void downloadFile(String fileName) throws IOException {
        sendPacket(clientSocket, new TransmissionPacket(CommandType.DOWNLOAD, fileName));
        tcpFileClientService.receiveFile(clientSocket, fileName);
        tcpFileClientService.printBitrate();
    }
}
