package com.example.lab2.entity.constants;

public enum CommandType {
    CONNECT("connect"),
    DISCONNECT("disconnect"),
    ECHO("echo"),
    UPLOAD("upload"),
    DOWNLOAD("download"),
    TIME("time"),
    HELP("help"),
    BLANK("");

    private final String value;

    CommandType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
