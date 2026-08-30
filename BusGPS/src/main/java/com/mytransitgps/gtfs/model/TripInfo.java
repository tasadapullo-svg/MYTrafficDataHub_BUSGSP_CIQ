package com.mytransitgps.gtfs.model;

/**
 * Static GTFS 班次信息，用于 Realtime trip、route、direction 和 shape 关联。
 */
public record TripInfo(
        String routeId,
        String serviceId,
        String tripId,
        String tripHeadsign,
        String tripShortName,
        Integer directionId,
        String shapeId) {
}
