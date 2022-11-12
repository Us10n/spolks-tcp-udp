package com.example.lab2.service.server;

import com.example.lab2.service.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Profile("server")
public class ServerService implements Service {

    @Autowired
    private TransferServerService transferServerService;

    @Override
    public void run() throws IOException {
        System.out.println("Server started");
        transferServerService.runHandler();
    }
}
