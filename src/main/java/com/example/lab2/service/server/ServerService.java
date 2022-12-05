package com.example.lab2.service.server;

import com.example.lab2.service.Service;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;

@Component
@Profile("server")
@RequiredArgsConstructor
public class ServerService implements Service {
    private final static int SERVER_PORT = 8888;
    private ServerSocket serverSocket;
    private final TransferServerService transferServerService;

    @Override
    public void run() throws IOException {
        serverSocket = new ServerSocket(SERVER_PORT);
        System.out.println("Server started");
        transferServerService.runServerHandler(serverSocket);
    }
}
