package com.mytransitgps.modules.ciqbus.dashboard.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** CIQBus 大屏只读 DTO；不承载采集或车辆匹配逻辑。 */
public final class CiqBusDashboardDtos {
    private CiqBusDashboardDtos() { }

    public record RealtimeEvent(String matchedEventId, String routeNo, String operatorCode,
                                String directionCode, Double latitude, Double longitude,
                                Instant lastSeenTime, Instant estimatedArrival, String sourceStopCode,
                                String entryStopCode, String exitStopCode, int observationCount,
                                Double confidence, String status) { }

    public record RealtimeResponse(boolean available, String message, Instant generatedAt,
                                   List<RealtimeEvent> events) { }

    public record Overview(LocalDate date, long sgToJb, long jbToSg, long total,
                           long incompleteCount, Map<String, Long> routeCounts,
                           Integer activeVehicleCount, Integer activeRouteCount,
                           Double avgCrossingSeconds, Double medianCrossingSeconds,
                           Double p90CrossingSeconds, Double p95CrossingSeconds,
                           Instant generatedAt) { }

    public record HourlyPoint(int hour, String label, long sgToJb, long jbToSg, long total,
                              Double avgCrossingSeconds, Double medianCrossingSeconds,
                              Double p90CrossingSeconds, Double p95CrossingSeconds) { }

    public record Passage(String matchedEventId, String routeNo, String operatorCode,
                          String directionCode, String stopCode, String stopName,
                          Instant passTime, Instant estimatedArrival, Double latitude,
                          Double longitude, int observationCount, Double confidence,
                          String matchMethod, Instant createTime) { }

    public record Crossing(String matchedEventId, String routeNo, String directionCode,
                           String entryStopCode, Instant ciqEntryTime, String exitStopCode,
                           Instant ciqExitTime, Integer crossingSeconds, Double crossingMinutes,
                           Double confidence, String eventStatus) { }

    public record Page<T>(int page, int size, long total, int totalPages, List<T> items) { }

    public record Status(String source, int collectionIntervalSeconds, List<String> routes,
                         Instant lastCollectionTime, Integer activeVehicleCount,
                         Integer activeRouteCount, String status, boolean realtimeAvailable,
                         String message, Instant generatedAt) { }
}
