package com.example.lab2.service.pool;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
@AllArgsConstructor
@NoArgsConstructor
public class ClientSocketPool {
    private Map<String, Socket> clientMap = new HashMap<>();

    public void putClientSocket(String key, Socket socket) {
        clientMap.put(key, socket);
    }

    public Socket getClientSocket(String key) {
        return clientMap.get(key);
    }

    public void removeClientSocket(String key) {
        clientMap.remove(key);
    }

    public Set<String> getClientSocketUids() {
        return clientMap.keySet();
    }

}
