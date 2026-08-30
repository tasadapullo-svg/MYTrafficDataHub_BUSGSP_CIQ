package com.mytransitgps.gtfs.util;

/**
 * GTFS 扩展时刻解析器，支持超过 24 小时的 HH:mm:ss 班次时间。
 */
public class GtfsTimeParser {

    public int parseToSeconds(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("GTFS time must not be blank.");
        }
        String[] parts = text.trim().split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid GTFS time: " + text);
        }
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        int seconds = Integer.parseInt(parts[2]);
        if (hours < 0 || minutes < 0 || minutes >= 60 || seconds < 0 || seconds >= 60) {
            throw new IllegalArgumentException("Invalid GTFS time: " + text);
        }
        return hours * 3600 + minutes * 60 + seconds;
    }
}
