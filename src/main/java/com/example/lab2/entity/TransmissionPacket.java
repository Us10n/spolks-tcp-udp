package com.example.lab2.entity;

import com.example.lab2.entity.constants.CommandType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class TransmissionPacket implements Serializable {
    private long minGuaranteedReceived = 0;
    private boolean isAck = false;
    private boolean isEof = false;
    private long numberOfPacket = 0;
    private long fileSize = 0;
    private CommandType commandType = CommandType.BLANK;
    private byte[] data = new byte[]{};
    private String fileName = "";


    public TransmissionPacket(CommandType commandType, String fileName) {
        this.commandType = commandType;
        this.fileName = fileName;
    }

    public TransmissionPacket(CommandType commandType, byte[] data) {
        this.commandType = commandType;
        this.data = data;
    }

    public TransmissionPacket(CommandType commandType, byte[] data, boolean isAck) {
        this.commandType = commandType;
        this.data = data;
        this.isAck = isAck;
    }

    public TransmissionPacket(CommandType commandType, byte[] data,
                              long numberOfPacket, String fileName, long fileSize) {
        this.commandType = commandType;
        this.data = data;
        this.numberOfPacket = numberOfPacket;
        this.fileName = fileName;
        this.fileSize = fileSize;
    }

    public TransmissionPacket(CommandType commandType) {
        this.commandType = commandType;
    }

    public TransmissionPacket(CommandType commandType, boolean isAck) {
        this.commandType = commandType;
        this.isAck = isAck;
    }

    public TransmissionPacket(CommandType commandType, boolean isAck, long numberOfPacket, String fileName, long fileSize) {
        this.commandType = commandType;
        this.isAck = isAck;
        this.numberOfPacket = numberOfPacket;
        this.fileName = fileName;
        this.fileSize = fileSize;
    }

    public TransmissionPacket(CommandType commandType, byte[] data,
                              long numberOfPacket, String fileName, long fileSize,
                              long minGuaranteedReceived) {
        this.commandType = commandType;
        this.data = data;
        this.numberOfPacket = numberOfPacket;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.minGuaranteedReceived = minGuaranteedReceived;
    }
}
