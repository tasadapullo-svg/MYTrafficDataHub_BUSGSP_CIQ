package com.mytransitgps.gtfs.model;

/**
 * Static GTFS 线路形状点，保留顺序、经纬度和累计距离。
 */
public record ShapePoint(
        String shapeId,
        Integer sequence,
        Double latitude,
        Double longitude) {
}
