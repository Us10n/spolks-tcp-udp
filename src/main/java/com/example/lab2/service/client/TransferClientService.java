package com.example.lab2.service.client;

import java.io.IOException;

public interface TransferClientService {
    void connectServer(String host, int port) throws IOException;

    void disconnectServer() throws IOException;

    String sendEcho(String echoMessage) throws IOException;

    String requestTime() throws IOException;

    void uploadFile(String commandArg) throws IOException;

    void downloadFile(String commandArg) throws IOException;
}
