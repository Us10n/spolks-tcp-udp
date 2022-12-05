package com.example.lab2.entity.constants;

public enum FileTransferStage {
    GREETING("GREETING"),
    META("META"),
    DATA_REQUEST("DATA_REQUEST"),
    DATA_RESPONSE("DATA_REPONSE"),
    INFO("INFO");

    private final String value;

    FileTransferStage(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
