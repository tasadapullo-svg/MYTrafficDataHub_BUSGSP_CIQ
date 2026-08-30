package com.mytransitgps.gtfs.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.mytransitgps.gtfs.model.RouteInfo;
import com.mytransitgps.gtfs.model.ScheduledTripTimeValidationResult;
import com.mytransitgps.gtfs.model.StaticFeedData;
import com.mytransitgps.gtfs.model.StopTimeInfo;
import com.mytransitgps.gtfs.model.TripInfo;
import com.mytransitgps.gtfs.util.GtfsTimeParser;
import com.mytransitgps.gtfs.util.StatsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 计划班次时间校验服务，比较车辆时间与 Static stop_times 的运行区间。
 */
public class ScheduledTripTimeValidationService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTripTimeValidationService.class);
    private final GtfsTimeParser timeParser;
    private final double maxTripHours;

    public ScheduledTripTimeValidationService() {
        this(new GtfsTimeParser(), 6.0d);
    }

    public ScheduledTripTimeValidationService(GtfsTimeParser timeParser, double maxTripHours) {
        this.timeParser = timeParser;
        this.maxTripHours = maxTripHours;
    }

    public ScheduledTripTimeValidationResult analyze(String feedId, StaticFeedData staticFeedData) {
        // GTFS 允许时刻超过 24:00，统一转换为服务日秒数后再比较首末站。
        List<Map<String, Object>> tripRows = new ArrayList<>();
        Map<RouteKey, List<Long>> durationsByRoute = new LinkedHashMap<>();
        Map<RouteKey, RouteAccumulator> routeAccumulators = new LinkedHashMap<>();
        Map<String, Integer> statusCounts = new LinkedHashMap<>();

        for (TripInfo trip : staticFeedData.trips().values()) {
            List<StopTimeInfo> stopTimes = staticFeedData.stopTimesByTrip().getOrDefault(trip.tripId(), List.of());
            TripTimingResult timing = analyzeTrip(stopTimes);
            RouteInfo route = staticFeedData.routes().get(trip.routeId());

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("feed_id", feedId);
            row.put("route_id", trip.routeId());
            row.put("route_short_name", route == null ? null : route.routeShortName());
            row.put("route_long_name", route == null ? null : route.routeLongName());
            row.put("trip_id", trip.tripId());
            row.put("direction_id", trip.directionId());
            row.put("shape_id", trip.shapeId());
            row.put("first_stop_sequence", timing.firstStopSequence());
            row.put("last_stop_sequence", timing.lastStopSequence());
            row.put("scheduled_start_seconds", timing.startSeconds());
            row.put("scheduled_end_seconds", timing.endSeconds());
            row.put("scheduled_duration_seconds", timing.durationSeconds());
            row.put("scheduled_duration_minutes", timing.durationSeconds() == null ? null : formatMinutes(timing.durationSeconds()));
            row.put("scheduled_time_qc_status", timing.status());
            tripRows.add(row);

            statusCounts.merge(timing.status(), 1, Integer::sum);
            RouteKey routeKey = new RouteKey(trip.routeId(), route == null ? null : route.routeShortName(), route == null ? null : route.routeLongName(), trip.directionId());
            RouteAccumulator routeAccumulator = routeAccumulators.computeIfAbsent(routeKey, ignored -> new RouteAccumulator());
            routeAccumulator.tripCount++;
            if ("VALID_SCHEDULE_DURATION".equals(timing.status()) && timing.durationSeconds() != null) {
                routeAccumulator.validTripCount++;
                durationsByRoute.computeIfAbsent(routeKey, ignored -> new ArrayList<>()).add(timing.durationSeconds());
            } else {
                routeAccumulator.invalidTripCount++;
            }
        }

        List<Map<String, Object>> routeSummaryRows = new ArrayList<>();
        for (Map.Entry<RouteKey, RouteAccumulator> entry : routeAccumulators.entrySet()) {
            List<Long> durations = durationsByRoute.getOrDefault(entry.getKey(), List.of());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("feed_id", feedId);
            row.put("route_id", entry.getKey().routeId());
            row.put("route_short_name", entry.getKey().routeShortName());
            row.put("route_long_name", entry.getKey().routeLongName());
            row.put("direction_id", entry.getKey().directionId());
            row.put("trip_count", entry.getValue().tripCount);
            row.put("valid_trip_count", entry.getValue().validTripCount);
            row.put("invalid_trip_count", entry.getValue().invalidTripCount);
            row.put("valid_rate", percentage(entry.getValue().validTripCount, entry.getValue().tripCount));
            row.put("duration_p50_min", toMinutes(StatsUtils.percentileLong(durations, 50)));
            row.put("duration_p90_min", toMinutes(StatsUtils.percentileLong(durations, 90)));
            row.put("duration_min", toMinutes(StatsUtils.minLong(durations)));
            row.put("duration_max", toMinutes(StatsUtils.maxLong(durations)));
            routeSummaryRows.add(row);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("feed_id", feedId);
        summary.put("trip_count", tripRows.size());
        summary.put("valid_trip_count", statusCounts.getOrDefault("VALID_SCHEDULE_DURATION", 0));
        summary.put("zero_duration_count", statusCounts.getOrDefault("ZERO_DURATION", 0));
        summary.put("all_stops_same_time_count", statusCounts.getOrDefault("ALL_STOPS_SAME_TIME", 0));
        summary.put("non_monotonic_time_count", statusCounts.getOrDefault("NON_MONOTONIC_TIME", 0));
        summary.put("missing_first_time_count", statusCounts.getOrDefault("MISSING_FIRST_TIME", 0));
        summary.put("missing_last_time_count", statusCounts.getOrDefault("MISSING_LAST_TIME", 0));
        summary.put("negative_duration_count", statusCounts.getOrDefault("NEGATIVE_DURATION", 0));
        summary.put("excessive_duration_count", statusCounts.getOrDefault("EXCESSIVE_DURATION", 0));
        int invalidCount = tripRows.size() - statusCounts.getOrDefault("VALID_SCHEDULE_DURATION", 0);
        log.info("Static计划班次时间校验完成，feedId={}，trips={}，valid={}，invalid={}，negative={}，zeroDuration={}，missingFirst={}，missingLast={}，excessive={}",
                feedId, tripRows.size(), statusCounts.getOrDefault("VALID_SCHEDULE_DURATION", 0), invalidCount,
                statusCounts.getOrDefault("NEGATIVE_DURATION", 0), statusCounts.getOrDefault("ZERO_DURATION", 0),
                statusCounts.getOrDefault("MISSING_FIRST_TIME", 0), statusCounts.getOrDefault("MISSING_LAST_TIME", 0),
                statusCounts.getOrDefault("EXCESSIVE_DURATION", 0));
        if (invalidCount > 0) {
            log.warn("Static计划班次时间存在异常，feedId={}，invalidTrips={}，原始GTFS时间继续保留", feedId, invalidCount);
        }

        return new ScheduledTripTimeValidationResult(summary, tripRows, routeSummaryRows);
    }

    private TripTimingResult analyzeTrip(List<StopTimeInfo> stopTimes) {
        Integer firstStopSequence = stopTimes.isEmpty() ? null : stopTimes.get(0).stopSequence();
        Integer lastStopSequence = stopTimes.isEmpty() ? null : stopTimes.get(stopTimes.size() - 1).stopSequence();
        Integer startSeconds = null;
        Integer endSeconds = null;
        boolean nonMonotonic = false;
        boolean allTimesSame = true;
        Integer previousTime = null;
        Integer firstObservedTime = null;

        for (StopTimeInfo stopTime : stopTimes) {
            Integer arrival = parseNullable(stopTime.arrivalTime());
            Integer departure = parseNullable(stopTime.departureTime());
            Integer firstUsable = departure != null ? departure : arrival;
            Integer lastUsable = arrival != null ? arrival : departure;
            if (startSeconds == null && firstUsable != null) {
                startSeconds = firstUsable;
                firstStopSequence = stopTime.stopSequence();
            }
            if (lastUsable != null) {
                endSeconds = lastUsable;
                lastStopSequence = stopTime.stopSequence();
            }

            Integer comparable = arrival != null ? arrival : departure;
            if (comparable != null) {
                if (firstObservedTime == null) {
                    firstObservedTime = comparable;
                } else if (!firstObservedTime.equals(comparable)) {
                    allTimesSame = false;
                }
                if (previousTime != null && comparable < previousTime) {
                    nonMonotonic = true;
                }
                previousTime = comparable;
            }
        }

        if (startSeconds == null) {
            return new TripTimingResult(firstStopSequence, lastStopSequence, null, endSeconds, null, "MISSING_FIRST_TIME");
        }
        if (endSeconds == null) {
            return new TripTimingResult(firstStopSequence, lastStopSequence, startSeconds, null, null, "MISSING_LAST_TIME");
        }
        long duration = (long) endSeconds - startSeconds;
        if (nonMonotonic) {
            return new TripTimingResult(firstStopSequence, lastStopSequence, startSeconds, endSeconds, duration, "NON_MONOTONIC_TIME");
        }
        if (duration < 0) {
            return new TripTimingResult(firstStopSequence, lastStopSequence, startSeconds, endSeconds, duration, "NEGATIVE_DURATION");
        }
        if (duration == 0) {
            return new TripTimingResult(firstStopSequence, lastStopSequence, startSeconds, endSeconds, duration, "ZERO_DURATION");
        }
        if (allTimesSame) {
            return new TripTimingResult(firstStopSequence, lastStopSequence, startSeconds, endSeconds, duration, "ALL_STOPS_SAME_TIME");
        }
        if (duration > maxTripHours * 3600.0d) {
            return new TripTimingResult(firstStopSequence, lastStopSequence, startSeconds, endSeconds, duration, "EXCESSIVE_DURATION");
        }
        return new TripTimingResult(firstStopSequence, lastStopSequence, startSeconds, endSeconds, duration, "VALID_SCHEDULE_DURATION");
    }

    private Integer parseNullable(String text) {
        return text == null || text.isBlank() ? null : timeParser.parseToSeconds(text);
    }

    private String percentage(int numerator, int denominator) {
        if (denominator == 0) {
            return "0.00%";
        }
        return String.format(Locale.ROOT, "%.2f%%", numerator * 100.0d / denominator);
    }

    private Double toMinutes(Long seconds) {
        return seconds == null ? null : formatMinutes(seconds);
    }

    private Double formatMinutes(long seconds) {
        return Math.round((seconds / 60.0d) * 100.0d) / 100.0d;
    }

    /** 路线与方向组成的汇总键。 */
    private record RouteKey(
            String routeId,
            String routeShortName,
            String routeLongName,
            Integer directionId) {
    }

    /** 按路线方向累加班次数量和计划运行时长。 */
    private static class RouteAccumulator {
        private int tripCount;
        private int validTripCount;
        private int invalidTripCount;
    }

    /** 单个 trip 首末站时刻及有效性的计算结果。 */
    private record TripTimingResult(
            Integer firstStopSequence,
            Integer lastStopSequence,
            Integer startSeconds,
            Integer endSeconds,
            Long durationSeconds,
            String status) {
    }
}
