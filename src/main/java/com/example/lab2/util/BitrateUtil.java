package com.example.lab2.util;

import java.util.ArrayList;
import java.util.List;

public class BitrateUtil {
    private List<Long> timeBorders;
    private Long fileSize;

    public BitrateUtil() {
        this.timeBorders = new ArrayList<>();
    }

    public void addTimeBorder(Long timeBorder) {
        timeBorders.add(timeBorder);
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public double countBitRate() {
        long totalTransferTime = 0;
        for (int i = 0; i < timeBorders.size(); i++) {
            if (i % 2 == 0) {
                totalTransferTime -= timeBorders.get(i);
            } else {
                totalTransferTime += timeBorders.get(i);
            }
        }

        return fileSize / totalTransferTime;
    }

    public void clearBorders() {
        timeBorders = new ArrayList<>();
    }
}
