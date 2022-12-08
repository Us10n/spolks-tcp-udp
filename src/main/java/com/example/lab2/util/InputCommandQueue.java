package com.example.lab2.util;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class InputCommandQueue {
    private Queue<String> commandQueue = new ConcurrentLinkedQueue<>();

    public void addCommand(String command) {
        commandQueue.offer(command);
    }

    public Optional<String> pollCommand() {
        return Optional.ofNullable(commandQueue.poll());
    }
}
