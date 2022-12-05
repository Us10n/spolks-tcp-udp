package com.example.lab2.entity;

import com.example.lab2.entity.constants.CommandType;
import com.example.lab2.entity.constants.FileTransferStage;
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
    private CommandType commandType;
    private byte[] data;
    private boolean isAck;
    private FileMeta fileMeta;
    private FileTransferStage fileTransferStage;

    public TransmissionPacket(CommandType commandType, byte[] data) {
        this.commandType = commandType;
        this.data = data;
    }

    public TransmissionPacket(CommandType commandType, byte[] data, boolean isAck) {
        this.commandType = commandType;
        this.data = data;
        this.isAck = isAck;
    }

    public TransmissionPacket(CommandType commandType) {
        this.commandType = commandType;
    }

    public TransmissionPacket(CommandType commandType, boolean isAck) {
        this.commandType = commandType;
        this.isAck = isAck;
    }

    public TransmissionPacket(CommandType commandType, FileMeta fileMeta) {
        this.commandType = commandType;
        this.fileMeta = fileMeta;
    }

    public TransmissionPacket(CommandType commandType, FileTransferStage fileTransferStage, FileMeta fileMeta) {
        this.commandType = commandType;
        this.fileMeta = fileMeta;
        this.fileTransferStage = fileTransferStage;
    }

    public TransmissionPacket(CommandType commandType, FileTransferStage fileTransferStage, boolean isAck) {
        this.commandType = commandType;
        this.fileTransferStage = fileTransferStage;
        this.isAck = isAck;
    }

    public TransmissionPacket(CommandType commandType, FileTransferStage fileTransferStage, byte[] data, FileMeta fileMeta) {
        this.commandType = commandType;
        this.fileTransferStage = fileTransferStage;
        this.data = data;
        this.fileMeta = fileMeta;
    }
}
