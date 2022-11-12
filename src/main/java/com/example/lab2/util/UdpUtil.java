package com.example.lab2.util;

import com.example.lab2.entity.TransmissionPacket;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Optional;

import static com.example.lab2.entity.constants.Constants.PACKET_SIZE;
import static com.example.lab2.util.Converter.convertBytesToTransmissionPacket;
import static com.example.lab2.util.Converter.convertObjectToBytes;

@Slf4j
public class UdpUtil {

    public static DatagramPacket createSendingPacket(byte[] sendingBuffer, InetAddress recipientAddress, int recipientPort) {
        return new DatagramPacket(sendingBuffer, sendingBuffer.length, recipientAddress, recipientPort);
    }

    public static DatagramPacket createReceivingPacket(byte[] receivingBuffer) {
        return new DatagramPacket(receivingBuffer, receivingBuffer.length);
    }

    public static void sendObject(DatagramSocket socket, InetAddress recipientAddress,
                                  Integer recipientPort, TransmissionPacket object) throws IOException {
        var dataArray = convertObjectToBytes(object);
        var datagramPacket = createSendingPacket(dataArray, recipientAddress, recipientPort);
        socket.send(datagramPacket);
    }

    public static DatagramPacket receiveDatagram(DatagramSocket socket) throws IOException {
        byte[] receivingBuffer = new byte[PACKET_SIZE];
        var receivingPacket = createReceivingPacket(receivingBuffer);
        socket.receive(receivingPacket);
        return receivingPacket;
    }

    public static Optional<TransmissionPacket> receivePacketWithTimeOut(DatagramSocket socket, int timeOut) throws IOException {
        try {
            socket.setSoTimeout(timeOut);
            byte[] receivingBuffer = new byte[PACKET_SIZE];
            var receivingPacket = createReceivingPacket(receivingBuffer);
            socket.receive(receivingPacket);
            return Optional.ofNullable(convertBytesToTransmissionPacket(receivingPacket.getData()));
        } catch (SocketTimeoutException ex) {
            return Optional.empty();
        } finally {
            socket.setSoTimeout(0);
        }
    }

    public static TransmissionPacket receivePacketAndSendAck(DatagramSocket socket) throws IOException {
        var datagram = receiveDatagram(socket);
        var packet = convertBytesToTransmissionPacket(datagram.getData());
        sendObject(socket, datagram.getAddress(), datagram.getPort(),
                new TransmissionPacket(packet.getCommandType(), true, packet.getNumberOfPacket(), packet.getFileName(), packet.getFileSize()));
        return packet;
    }

    public static Optional<TransmissionPacket> receivePacketWithTimeOutAndSendAck(DatagramSocket socket, int timeOut) throws IOException {
        socket.setSoTimeout(timeOut);
        int tries = 0;
        while (tries++ < 5) {
            try {
                var transmissionPacket = receivePacketAndSendAck(socket);
                socket.setSoTimeout(0);
                return Optional.of(transmissionPacket);
            } catch (SocketTimeoutException ex) {
                log.warn("TIMEOUT: Can't receive packet");
            }
        }
        socket.setSoTimeout(0);
        return Optional.empty();
    }

    public static Optional<TransmissionPacket> sendPacketAndReceiveAckWithTimeOut(DatagramSocket socket, InetAddress recipientAddress,
                                                                                  Integer recipientPort, TransmissionPacket packet,
                                                                                  int timeOut) throws IOException {
        socket.setSoTimeout(timeOut);
        int tries = 0;
        while (tries++ < 5) {
            try {
                sendObject(socket, recipientAddress, recipientPort, packet);
                var datagramPacket = receiveDatagram(socket);
                socket.setSoTimeout(0);
                return Optional.ofNullable(convertBytesToTransmissionPacket(datagramPacket.getData()));
            } catch (SocketTimeoutException ex) {
                log.warn("TIMEOUT: Can't receive ack");
            }
        }
        socket.setSoTimeout(0);
        return Optional.empty();
    }

}
