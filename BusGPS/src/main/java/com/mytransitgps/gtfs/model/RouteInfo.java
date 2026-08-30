package com.mytransitgps.gtfs.model;

/**
 * Static GTFS 路线信息，用于 Realtime 路线补全和一致性校验。
 */
public record RouteInfo(
        String routeId,
        String routeShortName,
        String routeLongName,
        String routeType,
        String routeColor,
        String routeTextColor) {
}
