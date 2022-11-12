package com.example.lab2.service.client;

import com.example.lab2.entity.constants.CommandType;
import com.example.lab2.service.Service;
import com.example.lab2.service.client.udp.UdpClientService;
import com.example.lab2.service.server.TransferServerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Scanner;

@Slf4j
@Component
@Profile("client")
public class ClientService implements Service {

    private static final String SPACE = " ";
    private boolean isConnectedToServer = false;

    @Autowired
    private TransferClientService transferClientService;

    public void run() {
        log.info("Client started");
        Scanner scanner = new Scanner(System.in);

        while (true) {
            try {
                System.out.print(">>> ");
                var commandString = scanner.nextLine().toLowerCase();
                String[] commandArgs = commandString.split(SPACE);
                var commandType = Arrays.stream(CommandType.values())
                        .filter(command -> command.getValue().equals(commandArgs[0]))
                        .findFirst()
                        .orElse(CommandType.BLANK);
                switch (commandType) {
                    case CONNECT -> {
                        if (!isConnectedToServer) {
                            String host = commandArgs[1];
                            int port = Integer.parseInt(commandArgs[2]);
                            transferClientService.connectServer(host, port);
                            isConnectedToServer = true;
                        } else {
                            log.warn("Client is already connected to server.");
                        }
                    }
                    case DISCONNECT -> {
                        if (isConnectedToServer) {
                            transferClientService.disconnectServer();
                            isConnectedToServer = false;
                        } else {
                            log.warn("Client is not connected to any server.");
                        }
                    }
                    case ECHO -> {
                        if (isConnectedToServer) {
                            var echoMessage = commandString.substring(commandArgs[0].length() + 1);
                            if (echoMessage.isBlank()) {
                                log.warn("Message is empty");
                                break;
                            }
                            var serverResponse = transferClientService.sendEcho(echoMessage);
                            log.info(serverResponse);
                        } else {
                            log.warn("Client is not connected to any server.");
                        }
                    }
                    case TIME -> {
                        if (isConnectedToServer) {
                            var time = transferClientService.requestTime();
                            log.info(time);
                        } else {
                            log.warn("Client is not connected to any server.");
                        }
                    }

                    case UPLOAD -> {
                        if (isConnectedToServer) {
                            if (commandArgs.length != 2) {
                                log.warn("No file name specified");
                                break;
                            }
                            transferClientService.uploadFile(commandArgs[1]);
                        } else {
                            log.warn("Client is not connected to any server.");
                        }
                    }
                    case DOWNLOAD -> {
                        if (isConnectedToServer) {
                            if (commandArgs.length != 2) {
                                log.warn("No file name specified");
                                break;
                            }
                            transferClientService.downloadFile(commandArgs[1]);
                        } else {
                            log.warn("Client is not connected to any server.");
                        }
                    }
//
//                    case CommandType.UPLOAD -> {
//                        if (isConnectedToServer) {
//                            clientSocket.setSoTimeout(TimeOut.UPLOAD);
//                            if (commandArgs.length != 2) {
//                                System.out.println("No file name specified");
//                                break;
//                            }
//                            fileIOService.sendFile(command, commandArgs[1], in, out);
//                        } else {
//                            System.out.println("Client is not connected to any server.");
//                        }
//                    }
//
//                    case CommandType.DOWNLOAD -> {
//                        if (isConnectedToServer) {
//                            clientSocket.setSoTimeout(TimeOut.DOWNLOAD);
//                            if (commandArgs.length != 2) {
//                                System.out.println("No file name specified");
//                                break;
//                            }
//                            fileIOService.receiveFile(command, commandArgs[1], in, out);
////                            fileIOService.printBitrate();
////                            fileIOService.clearBorders();
//                        } else {
//                            System.out.println("Client is not connected to any server.");
//                        }
//                    }
                    case HELP -> printHelp();

                    default -> log.warn("No command found");
                }
            } catch (Exception e) {
                log.error("Unknown exception");
                e.printStackTrace();
                isConnectedToServer = false;
            }
        }
    }


    private void printHelp() {
        StringBuilder helpBuilder = new StringBuilder();
        helpBuilder.append("- ").append(CommandType.HELP).append(" - print help;\n");
        helpBuilder.append("- ").append(CommandType.CONNECT).append(" host port - connect to server;\n");
        helpBuilder.append("- ").append(CommandType.DISCONNECT).append(" - disconnect from connected server;\n");
        helpBuilder.append("- ").append(CommandType.ECHO).append(" message - ex \"echo hello world\";\n");
        helpBuilder.append("- ").append(CommandType.TIME).append(" - get server time;\n");
        helpBuilder.append("- ").append(CommandType.UPLOAD).append(" filename.format - send file to server ex \"upload text.txt\";\n");
        helpBuilder.append("- ").append(CommandType.DOWNLOAD).append(" filename.format - download file from server ex \"download text.txt\"-;\n");
        System.out.println(helpBuilder);
    }
}
