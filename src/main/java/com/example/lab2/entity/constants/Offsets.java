package com.example.lab2.entity.constants;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Offsets {
    private Long serverReceived = 0L;
    private Long clientReceived = 0L;
    private String lastFileClientReceived = "";
    private String lastFileServerReceived = "";
}
