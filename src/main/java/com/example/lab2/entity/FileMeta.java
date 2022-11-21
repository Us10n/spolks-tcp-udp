package com.example.lab2.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileMeta implements Serializable {
    private Boolean canRead;
    private Long primaryReadOffset;
}
