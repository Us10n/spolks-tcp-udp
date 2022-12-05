package com.example.lab2.util;

import com.example.lab2.entity.TransmissionPacket;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
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

    public static TransmissionPacket receivePacketWithTimeOutRetries(Socket socket, int timeOut) throws IOException {
        int tries = 0;
        while (tries++ < 5) {
            socket.setSoTimeout(timeOut);
            try {
                return receivePacket(socket);
            } catch (IOException ex) {
                log.warn("TIMEOUT: Can't receive packet");
            } finally {
                socket.setSoTimeout(0);
            }
        }
        throw new SocketTimeoutException();
    }

    public static TransmissionPacket receivePacketWithTimeOut(Socket socket, int timeOut) throws IOException {
        socket.setSoTimeout(timeOut);
        try {
            return receivePacket(socket);
        } catch (IOException ex) {
            log.warn("TIMEOUT: Can't receive packet");
        } finally {
            socket.setSoTimeout(0);
        }
        throw new SocketTimeoutException();
    }

    public static Optional<TransmissionPacket> receiveCommandWithTimeOut(Socket socket, int timeOut) throws IOException {
        socket.setSoTimeout(timeOut);
        try {
            var datagramPacket = receivePacket(socket);
            return Optional.of(datagramPacket);
        } catch (IOException ex) {
            return Optional.empty();
        } finally {
            socket.setSoTimeout(0);
        }
    }

    public static Optional<Socket> acceptClientWithTimeOut(ServerSocket serverSocket, int timeOut) throws IOException {
        serverSocket.setSoTimeout(timeOut);
        try {
            var clientSocket = serverSocket.accept();
            return Optional.of(clientSocket);
        } catch (IOException ignored) {
            return Optional.empty();
        } finally {
            serverSocket.setSoTimeout(0);
        }
    }

    public static String generateUid(Socket socket) {
        return socket.getInetAddress() + ":" + socket.getPort();
    }
}
