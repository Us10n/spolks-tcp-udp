package com.example.lab2.entity.constants;

import lombok.Value;

@Value
public class Constants {
    public static final int BUFFER_SIZE = 10000;
    public static final int PACKET_SIZE = BUFFER_SIZE + 495;
    public static final int MIN_POOL_SIZE = 5;
    public static final int MAX_POOL_SIZE = 10;
}
