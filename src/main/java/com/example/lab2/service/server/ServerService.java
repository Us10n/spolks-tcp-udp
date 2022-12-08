package com.example.lab2.service.server;

import com.example.lab2.service.Service;
import com.example.lab2.service.server.tcp.TcpServerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

import static com.example.lab2.entity.constants.Constants.MAX_POOL_SIZE;
import static com.example.lab2.entity.constants.Constants.MIN_POOL_SIZE;

@Slf4j
@Component
@Profile("server")
public class ServerService implements Service {
    private final static int SERVER_PORT = 8888;
    private ServerSocket serverSocket;
    private ExecutorService executorService;

    @Override
    public void run() throws IOException {
        serverSocket = new ServerSocket(SERVER_PORT);
        executorService = new ThreadPoolExecutor(1, 1,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<>());
        System.out.println("Server started");
        while (true) {
            Socket clientSocket = null;
            try {
                clientSocket = serverSocket.accept();
                log.info("Client connected: " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());
                executorService.submit(new TcpServerService(clientSocket));
            } catch (RejectedExecutionException e) {
                log.warn("Thread pool is full");
                log.info("Client disconnected: " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());
                clientSocket.close();
            }
        }
    }
}
