package com.example.lab2.util;

import com.example.lab2.entity.TransmissionPacket;
import org.springframework.util.SerializationUtils;


public class Converter {
    public static byte[] convertObjectToBytes(Object object) {
        return SerializationUtils.serialize(object);
    }

    public static Object convertBytesToObject(byte[] bytes) {
        return SerializationUtils.deserialize(bytes);
    }

    public static TransmissionPacket convertBytesToTransmissionPacket(byte[] bytes) {
        return (TransmissionPacket) convertBytesToObject(bytes);
    }
}
