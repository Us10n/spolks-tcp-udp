package com.example.lab2.util;

import com.example.lab2.entity.TransmissionPacket;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Optional;

@Slf4j
public class TcpUtil {

    public static void sendPacket(Socket socket, TransmissionPacket packet) throws IOException {
        new ObjectOutputStream(socket.getOutputStream()).writeObject(packet);
    }

    public static TransmissionPacket receivePacket(Socket socket) throws IOException {
        try {
            return (TransmissionPacket) new ObjectInputStream(socket.getInputStream()).readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException();
        }
    }

    public static Optional<TransmissionPacket> receivePacketWithTimeOut(Socket socket, int timeOut) throws IOException {
        socket.setSoTimeout(timeOut);
        int tries = 0;
        while (tries++ < 5) {
            try {
                var datagramPacket = receivePacket(socket);
                socket.setSoTimeout(0);
                return Optional.of(datagramPacket);
            } catch (SocketTimeoutException ex) {
                log.warn("TIMEOUT: Can't receive packet");
            }
        }
        socket.setSoTimeout(0);
        return Optional.empty();
    }
}
