package com.example.lab2.service.server;

import com.example.lab2.entity.TransmissionPacket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public interface TransferServerService {
    void runServerHandler(ServerSocket serverSocket) throws IOException;

    void runCommandHandler(Socket socket) throws IOException;

    void sendEcho(Socket clientSocket, TransmissionPacket receivedCommand) throws IOException;

    void sendTime(Socket clientSocket, TransmissionPacket receivedCommand) throws IOException;

    void receiveFile(Socket clientSocket, TransmissionPacket receivedCommand) throws IOException;

    void sendFile(Socket clientSocket, TransmissionPacket receivedCommand) throws IOException;
}
