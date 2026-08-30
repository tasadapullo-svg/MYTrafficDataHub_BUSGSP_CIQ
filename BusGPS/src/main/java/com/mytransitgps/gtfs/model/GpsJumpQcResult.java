package com.mytransitgps.gtfs.model;

/**
 * 相邻车辆位置的 GPS 跳点质检结果及跳过原因。
 */
public record GpsJumpQcResult(
        Long previousVehicleTimestamp,
        Long vehicleGapSeconds,
        String gapStatus,
        Long timeDeltaSeconds,
        Double distanceFromPreviousM,
        Double derivedSpeedKmh,
        String gpsJumpStatus,
        String jumpCalculationSkippedReason) {
}
