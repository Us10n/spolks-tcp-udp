package com.example.lab2.entity.constants;

import lombok.Value;

import java.util.concurrent.TimeUnit;

@Value
public class TimeOut {
    public static final Integer CONNECTION = Math.toIntExact(TimeUnit.SECONDS.toMillis(2));
    public static final Integer DISCONNECTION = Math.toIntExact(TimeUnit.SECONDS.toMillis(2));
    public static final Integer DOWNLOAD = Math.toIntExact(TimeUnit.SECONDS.toMillis(5));
    public static final Integer UPLOAD = Math.toIntExact(TimeUnit.SECONDS.toMillis(5));
    public static final Integer ECHO = Math.toIntExact(TimeUnit.SECONDS.toMillis(2));
    public static final Integer TIME = Math.toIntExact(TimeUnit.SECONDS.toMillis(2));
}
