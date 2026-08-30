package com.mytransitgps.persistence.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mytransitgps.gtfs.model.FeedSpatialBounds;
import com.mytransitgps.gtfs.model.GpsJumpQcResult;
import com.mytransitgps.gtfs.model.PositionQcResult;
import com.mytransitgps.gtfs.service.GpsJumpQcService;
import com.mytransitgps.gtfs.service.ObservationEligibilityPolicy;
import com.mytransitgps.gtfs.service.PositionQcService;
import com.mytransitgps.gtfs.util.HashUtils;
import com.mytransitgps.persistence.model.SnapshotQcSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 中文名称：Johor Bahru Realtime 车辆 QC 注释器。
 *
 * 功能说明：复用现有 PositionQcService、GpsJumpQcService 和 EligibilityPolicy，
 * 为每轮 enriched JSON 增加重复、时间、位置、跳点和可用性字段；异常只标记和告警，不过滤事实。
 */
public class JohorRealtimeQcAnnotator {

    private static final Logger log = LoggerFactory.getLogger(JohorRealtimeQcAnnotator.class);
    private final PositionQcService positionQcService = new PositionQcService();
    private final GpsJumpQcService gpsJumpQcService = new GpsJumpQcService(100.0d, 130.0d, 600L);
    private final ObservationEligibilityPolicy eligibilityPolicy = new ObservationEligibilityPolicy();
    private final Set<String> seenObservationIdentities = new LinkedHashSet<>();
    private final Map<String, Previous> previousByVehicle = new LinkedHashMap<>();

    public SnapshotQcSummary annotate(String feedId, String snapshotUid, List<Map<String, Object>> vehicles,
                                      Instant ingestTime, FeedSpatialBounds bounds) {
        int qcCount = 0;
        int duplicates = 0;
        int staticMatched = 0;
        int anomalyVehicles = 0;
        for (int sequence = 0; sequence < vehicles.size(); sequence++) {
            Map<String, Object> vehicle = vehicles.get(sequence);
            vehicle.put("entity_sequence", sequence);
            String vehicleId = string(vehicle.get("vehicle_id"));
            Long timestamp = longValue(vehicle.get("vehicle_timestamp"));
            Double latitude = decimal(vehicle.get("latitude"));
            Double longitude = decimal(vehicle.get("longitude"));
            String observationKey = HashUtils.sha256Hex((feedId + "|" + string(vehicle.get("entity_id")) + "|" + vehicleId + "|"
                    + string(vehicle.get("trip_id")) + "|" + string(vehicle.get("realtime_route_id")) + "|"
                    + latitude + "|" + longitude + "|" + timestamp).getBytes(StandardCharsets.UTF_8));
            vehicle.put("observation_key", observationKey);
            String identity = vehicleId != null && timestamp != null ? feedId + "|" + vehicleId + "|" + timestamp : observationKey;
            boolean duplicate = !seenObservationIdentities.add(identity);
            vehicle.put("duplicate_observation", duplicate);

            LinkedHashSet<String> flags = new LinkedHashSet<>();
            if (duplicate) { flags.add("DUPLICATE_OBSERVATION"); duplicates++; }
            if (vehicleId == null) flags.add("MISSING_VEHICLE_ID");
            if (vehicle.get("trip_id") == null) flags.add("MISSING_TRIP_ID");
            if (vehicle.get("realtime_route_id") == null) flags.add("MISSING_ROUTE_ID");
            if (!Boolean.TRUE.equals(vehicle.get("route_resolved"))) { flags.add("ROUTE_UNRESOLVED"); log.warn("检测到Static Route未匹配，feedId={}，snapshotUid={}，vehicleId={}，原始观测继续保存", feedId, snapshotUid, vehicleId); }
            if (!Boolean.TRUE.equals(vehicle.get("trip_static_matched"))) { flags.add("TRIP_UNMATCHED"); log.warn("检测到Static Trip未匹配，feedId={}，snapshotUid={}，vehicleId={}，原始观测继续保存", feedId, snapshotUid, vehicleId); }
            else staticMatched++;

            PositionQcResult position = positionQcService.evaluate(latitude, longitude, bounds);
            vehicle.put("position_present", position.positionPresent()); vehicle.put("position_wgs84_valid", position.positionWgs84Valid());
            vehicle.put("zero_zero_position", position.zeroZeroPosition()); vehicle.put("feed_bounds_valid", position.feedBoundsValid());
            vehicle.put("position_qc_status", position.positionQcStatus());
            if (!"VALID_POSITION".equals(position.positionQcStatus())) {
                flags.add(position.positionQcStatus());
                log.warn("检测到车辆位置异常，feedId={}，snapshotUid={}，vehicleId={}，status={}，原始经纬度继续保存", feedId, snapshotUid, vehicleId, position.positionQcStatus());
            }

            Long freshness = timestamp == null ? null : Duration.between(Instant.ofEpochSecond(timestamp), ingestTime).getSeconds();
            vehicle.put("freshness_seconds", freshness);
            if (freshness != null && freshness < 0) { flags.add("FUTURE_TIMESTAMP"); log.warn("检测到Future Timestamp，feedId={}，snapshotUid={}，vehicleId={}，vehicleTime={}，原始时间戳继续保存", feedId, snapshotUid, vehicleId, timestamp); }

            Previous previous = vehicleId == null ? null : previousByVehicle.get(vehicleId);
            if (previous != null && previous.timestamp != null && timestamp != null) {
                if (timestamp.equals(previous.timestamp) && samePosition(previous, latitude, longitude)) flags.add("STALE_TIMESTAMP");
                else if (timestamp.equals(previous.timestamp)) flags.add("INCONSISTENT_TIMESTAMP_POSITION");
                else if (timestamp < previous.timestamp) flags.add("TIMESTAMP_REGRESSION");
                else if (samePosition(previous, latitude, longitude)) flags.add("STATIONARY");
            }

            GpsJumpQcResult jump = gpsJumpQcService.evaluate(previous == null ? null : previous.timestamp,
                    previous == null ? null : previous.latitude, previous == null ? null : previous.longitude,
                    previous != null && previous.positionValid, timestamp, latitude, longitude, position.transitPositionValid());
            vehicle.put("previous_vehicle_timestamp", jump.previousVehicleTimestamp()); vehicle.put("vehicle_gap_seconds", jump.vehicleGapSeconds());
            vehicle.put("gap_status", jump.gapStatus()); vehicle.put("time_delta_seconds", jump.timeDeltaSeconds());
            vehicle.put("distance_from_previous_m", jump.distanceFromPreviousM()); vehicle.put("derived_speed_kmh", jump.derivedSpeedKmh());
            vehicle.put("gps_jump_status", jump.gpsJumpStatus()); vehicle.put("jump_calculation_skipped_reason", jump.jumpCalculationSkippedReason());
            if ("LONG_GAP".equals(jump.gapStatus())) flags.add("LONG_GAP");
            if ("SUSPICIOUS_SPEED".equals(jump.gpsJumpStatus()) || "GPS_JUMP".equals(jump.gpsJumpStatus())) {
                flags.add(jump.gpsJumpStatus());
                log.warn("检测到GPS速度异常，feedId={}，snapshotUid={}，vehicleId={}，status={}，derivedSpeedKmh={}，原始观测继续保存",
                        feedId, snapshotUid, vehicleId, jump.gpsJumpStatus(), jump.derivedSpeedKmh());
            }
            ObservationEligibilityPolicy.Eligibility eligibility = eligibilityPolicy.evaluate(flags, position.positionQcStatus(), jump.gpsJumpStatus());
            vehicle.put("analysis_eligible", eligibility.analysisEligible()); vehicle.put("spatial_eligible", eligibility.spatialEligible());
            vehicle.put("qc_flags", List.copyOf(flags));
            qcCount += flags.size(); if (!flags.isEmpty()) anomalyVehicles++;
            if (vehicleId != null) previousByVehicle.put(vehicleId, new Previous(timestamp, latitude, longitude, position.transitPositionValid()));
        }
        return new SnapshotQcSummary(vehicles.size(), qcCount, duplicates, staticMatched, anomalyVehicles);
    }

    private boolean samePosition(Previous previous, Double lat, Double lon) { return previous.latitude != null && previous.longitude != null && lat != null && lon != null && Double.compare(previous.latitude, lat) == 0 && Double.compare(previous.longitude, lon) == 0; }
    private String string(Object value) { return value == null || value.toString().isBlank() ? null : value.toString(); }
    private Long longValue(Object value) { return value instanceof Number number ? number.longValue() : null; }
    private Double decimal(Object value) { return value instanceof Number number ? number.doubleValue() : null; }
    /** 单车辆上一条观测的时间、位置和状态缓存。 */
    private record Previous(Long timestamp, Double latitude, Double longitude, boolean positionValid) { }
}
