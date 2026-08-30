package com.mytransitgps.gtfs.model;

/**
 * Static GTFS 站序时刻信息，保留到离站时间和站点顺序。
 */
public record StopTimeInfo(
        String tripId,
        Integer stopSequence,
        String stopId,
        String arrivalTime,
        String departureTime) {
}
