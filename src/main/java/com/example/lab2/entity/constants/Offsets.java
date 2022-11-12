package com.example.lab2.entity.constants;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Offsets {
    private long serverReceived = 0;
    private long clientReceived = 0;
}
