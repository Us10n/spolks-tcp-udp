package com.example.lab2.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Scanner;

@Component
@RequiredArgsConstructor
public class NonBlockingInputReader implements Runnable {
    private final InputCommandQueue inputCommandQueue;
    private Scanner scanner = new Scanner(System.in);

    @Override
    public void run() {
        while (true) {
            var commandString = scanner.nextLine().toLowerCase();
            inputCommandQueue.addCommand(commandString);
        }
    }
}
