package com.mytransitgps.gtfs.model;

/**
 * Static GTFS 站点信息，用于停靠站关联和空间分析。
 */
public record StopInfo(
        String stopId,
        String stopName,
        Double stopLat,
        Double stopLon) {
}
