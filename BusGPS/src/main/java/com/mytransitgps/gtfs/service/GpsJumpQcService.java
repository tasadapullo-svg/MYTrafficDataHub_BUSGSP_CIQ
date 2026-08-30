package com.mytransitgps.gtfs.service;

import com.mytransitgps.gtfs.model.GpsJumpQcResult;
import com.mytransitgps.gtfs.util.GeoUtils;

/**
 * GPS 跳点质检服务，通过相邻位置距离、时间差和推导速度标记异常移动。
 */
public class GpsJumpQcService {

    private final double suspiciousSpeedKmh;
    private final double jumpSpeedKmh;
    private final long maxComparableGapSeconds;

    public GpsJumpQcService() {
        this(100.0d, 130.0d, 600L);
    }

    public GpsJumpQcService(double suspiciousSpeedKmh, double jumpSpeedKmh, long maxComparableGapSeconds) {
        this.suspiciousSpeedKmh = suspiciousSpeedKmh;
        this.jumpSpeedKmh = jumpSpeedKmh;
        this.maxComparableGapSeconds = maxComparableGapSeconds;
    }

    public GpsJumpQcResult evaluate(
            Long previousTimestamp,
            Double previousLat,
            Double previousLon,
            boolean previousPositionValid,
            Long currentTimestamp,
            Double currentLat,
            Double currentLon,
            boolean currentPositionValid) {
        // 只有前后时间和位置均可信时才推导速度，否则返回明确的跳过原因。
        Long gapSeconds = previousTimestamp != null && currentTimestamp != null ? currentTimestamp - previousTimestamp : null;
        String gapStatus = classifyGap(gapSeconds);

        if (!previousPositionValid || !currentPositionValid) {
            return new GpsJumpQcResult(previousTimestamp, gapSeconds, gapStatus, null, null, null, "INVALID_POSITION", "INVALID_POSITION");
        }
        if (previousTimestamp == null || currentTimestamp == null) {
            return new GpsJumpQcResult(previousTimestamp, gapSeconds, gapStatus, null, null, null, "NOT_AVAILABLE", "MISSING_TIMESTAMP");
        }
        long timeDelta = currentTimestamp - previousTimestamp;
        if (timeDelta <= 0) {
            return new GpsJumpQcResult(previousTimestamp, gapSeconds, gapStatus, timeDelta, null, null, "NOT_AVAILABLE", "NON_POSITIVE_DELTA");
        }
        if (timeDelta > maxComparableGapSeconds) {
            return new GpsJumpQcResult(previousTimestamp, gapSeconds, gapStatus, timeDelta, null, null, "LONG_GAP", "LONG_GAP");
        }

        double distanceMeters = GeoUtils.haversineMeters(previousLat, previousLon, currentLat, currentLon);
        double speedKmh = (distanceMeters / timeDelta) * 3.6d;
        String status = speedKmh > jumpSpeedKmh
                ? "GPS_JUMP"
                : speedKmh > suspiciousSpeedKmh ? "SUSPICIOUS_SPEED" : "NORMAL";
        return new GpsJumpQcResult(previousTimestamp, gapSeconds, gapStatus, timeDelta, distanceMeters, speedKmh, status, null);
    }

    public String classifyGap(Long gapSeconds) {
        if (gapSeconds == null) {
            return "NOT_AVAILABLE";
        }
        if (gapSeconds <= 180L) {
            return "NORMAL_GAP";
        }
        if (gapSeconds <= 300L) {
            return "MINOR_GAP";
        }
        if (gapSeconds <= 600L) {
            return "MODERATE_GAP";
        }
        return "LONG_GAP";
    }
}
