package com.example.lab2.entity.constants;

import lombok.Value;

@Value
public class Constants {
    public static final int BUFFER_SIZE = 1024;
    public static final int PACKET_SIZE = BUFFER_SIZE + 495;
}
