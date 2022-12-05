package com.example.lab2.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class FileMeta implements Serializable {
    private String fileName;
    private long fileSize;
    private long numberOfLastReceivedBlock;
    private long numberOfSendingBlock;
    private long numberOfRequestingBlock;
    private boolean isEof;
    private boolean canRead;
}
