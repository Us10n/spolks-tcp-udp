package com.example.lab2.service.server;

import com.example.lab2.entity.TransmissionPacket;

import java.io.IOException;

public interface TransferServerService {
    void runHandler() throws IOException;

    TransmissionPacket readCommand() throws IOException;

    void sendEcho(TransmissionPacket receivedCommand) throws IOException;

    void sendTime(TransmissionPacket receivedCommand) throws IOException;

    void connect(TransmissionPacket receivedCommand) throws IOException;

    void receiveFile(TransmissionPacket receivedCommand) throws IOException;

    void sendFile(TransmissionPacket receivedCommand) throws IOException;
}
