package com.example.lab2.entity.constants;

import lombok.Value;

//@Value
//public class CommandType {
//    public static final String CONNECT = "connect";
//    public static final String DISCONNECT = "disconnect";
//    public static final String ECHO = "echo";
//    public static final String UPLOAD = "upload";
//    public static final String DOWNLOAD = "download";
//    public static final String TIME = "time";
//    public static final String HELP = "help";
//}

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
