package com.example.lab2.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.InetAddress;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SocketMeta {
    private InetAddress address;
    private Integer port;
}
