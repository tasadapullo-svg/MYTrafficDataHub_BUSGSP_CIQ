package com.mytransitgps.persistence.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.mytransitgps.gtfs.util.GtfsTimeParser;
import com.mytransitgps.persistence.entity.JbVehicleLatestStateEntity;
import com.mytransitgps.persistence.entity.JbVehicleObservationEntity;
import com.mytransitgps.persistence.entity.JbVehicleObservationQcEntity;
import com.mytransitgps.persistence.model.ObservationMappingResult;
import com.mytransitgps.persistence.model.StaticReferenceIndex;
import com.mytransitgps.persistence.typehandler.PostgisGeometry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 中文名称：磁盘 enriched JSON 到车辆数据库实体映射器。
 *
 * 功能说明：只接受重新从磁盘反序列化得到的 JsonNode，逐 Entity occurrence 映射
 * jb.vehicle_observation 的全部 79 列语义，并生成 QC 与 Latest State 数据。
 */
@Service
@ConditionalOnProperty(prefix = "mytransitgps.database", name = "enabled", havingValue = "true")
public class VehicleObservationDatabaseMapper {

    private static final Logger log = LoggerFactory.getLogger(VehicleObservationDatabaseMapper.class);
    private static final DateTimeFormatter GTFS_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    public ObservationMappingResult map(JsonNode diskRoot, UUID feedUid, UUID runUid, UUID requestUid,
                                        UUID snapshotUid, StaticReferenceIndex references) {
        // 先验证顶层契约，防止结构不完整的 enriched JSON 产生部分数据库行。
        log.info("开始将磁盘enriched JSON映射为数据库实体，runUid={}，requestUid={}，snapshotUid={}，feedUid={}",
                runUid, requestUid, snapshotUid, feedUid);
        JsonNode snapshot = requiredObject(diskRoot, "snapshot");
        JsonNode vehicles = diskRoot.get("vehicles");
        if (vehicles == null || !vehicles.isArray()) {
            throw new IllegalArgumentException("enriched_full.json vehicles must be an array.");
        }
        Instant ingestTime = instant(snapshot, "ingest_timestamp_utc");
        List<JbVehicleObservationEntity> observations = new ArrayList<>();
        List<JbVehicleObservationQcEntity> qcRows = new ArrayList<>();
        List<JbVehicleLatestStateEntity> latestStates = new ArrayList<>();

        int sequence = 0;
        for (JsonNode vehicle : vehicles) {
            // 每个 Realtime entity occurrence 都映射成独立历史观测，不按 vehicle_id 去重。
            if (!vehicle.isObject()) {
                throw new IllegalArgumentException("Vehicle entry must be an object at sequence " + sequence);
            }
            JbVehicleObservationEntity row = mapObservation(vehicle, sequence, feedUid, runUid, requestUid,
                    snapshotUid, ingestTime, references);
            observations.add(row);
            ArrayNode flags = row.qcFlags != null && row.qcFlags.isArray()
                    ? (ArrayNode) row.qcFlags : JsonNodeFactory.instance.arrayNode();
            // qc_flags 数组是事实来源；明细表逐标记展开，便于按类别和严重度查询。
            for (JsonNode flag : flags) {
                String code = flag.asText();
                if (code.isBlank() || "VALID".equals(code)) {
                    continue;
                }
                qcRows.add(qcRow(row.uid, code, vehicle));
            }
            if (row.vehicleId != null && !row.vehicleId.isBlank()) {
                latestStates.add(latestState(row));
            }
            sequence++;
        }
        log.info("磁盘enriched JSON数据库实体映射完成，snapshotUid={}，observations={}，qcRows={}，latestStates={}",
                snapshotUid, observations.size(), qcRows.size(), latestStates.size());
        return new ObservationMappingResult(List.copyOf(observations), List.copyOf(qcRows), List.copyOf(latestStates));
    }

    private JbVehicleObservationEntity mapObservation(JsonNode v, int sequence, UUID feedUid, UUID runUid,
                                                       UUID requestUid, UUID snapshotUid, Instant ingestTime,
                                                       StaticReferenceIndex refs) {
        JbVehicleObservationEntity row = new JbVehicleObservationEntity();
        row.uid = UUID.randomUUID(); row.feedUid = feedUid; row.runUid = runUid; row.requestUid = requestUid; row.snapshotUid = snapshotUid;
        row.staticVersionUid = refs.staticVersionUid();
        row.entitySequence = sequence; row.entityId = text(v, "entity_id"); row.vehicleId = text(v, "vehicle_id");
        row.vehicleLabel = text(v, "vehicle_label"); row.licensePlate = text(v, "license_plate"); row.wheelchairAccessible = text(v, "wheelchair_accessible");
        row.tripId = text(v, "trip_id"); row.tripStartTimeRaw = text(v, "trip_start_time"); row.tripStartSeconds = gtfsSeconds(row.tripStartTimeRaw);
        row.tripStartDateRaw = text(v, "trip_start_date"); row.tripStartDate = gtfsDate(row.tripStartDateRaw);
        row.realtimeScheduleRelationship = text(v, "realtime_schedule_relationship"); row.realtimeRouteId = text(v, "realtime_route_id");
        row.staticRouteId = text(v, "static_route_id"); row.resolvedRouteId = text(v, "resolved_route_id");
        row.routeShortName = text(v, "route_short_name"); row.routeLongName = text(v, "route_long_name"); row.routeType = integer(v, "route_type");
        row.routeColor = text(v, "route_color"); row.routeTextColor = text(v, "route_text_color"); row.routeResolutionMethod = text(v, "route_resolution_method");
        row.realtimeRouteDirectMatched = bool(v, "realtime_route_direct_matched"); row.routeResolved = bool(v, "route_resolved");
        row.staticServiceId = text(v, "static_service_id"); row.tripHeadsign = text(v, "trip_headsign"); row.tripShortName = text(v, "trip_short_name");
        row.shapeId = text(v, "shape_id"); row.tripStaticMatched = bool(v, "trip_static_matched");
        row.realtimeDirectionId = shortInteger(v, "realtime_direction_id"); row.staticDirectionId = shortInteger(v, "static_direction_id"); row.directionComparison = text(v, "direction_comparison");
        row.latitude = decimal(v, "latitude"); row.longitude = decimal(v, "longitude"); row.bearing = decimal(v, "bearing"); row.odometerM = decimal(v, "odometer_m");
        row.sourceSpeedRaw = decimal(v, "source_speed_raw"); row.sourceSpeedPresent = requiredBoolean(v, "source_speed_present");
        row.sourceSpeedUnitDeclared = text(v, "source_speed_unit_declared"); row.sourceSpeedUnitInterpretation = text(v, "source_speed_unit_interpretation");
        row.currentStopSequence = integer(v, "current_stop_sequence"); row.stopId = text(v, "stop_id"); row.currentStatus = text(v, "current_status");
        row.congestionLevel = text(v, "congestion_level"); row.occupancyStatus = text(v, "occupancy_status"); row.occupancyPercentage = integer(v, "occupancy_percentage");
        row.multiCarriageDetails = copy(v.get("multi_carriage_details")); row.vehicleTimestampRaw = longValue(v, "vehicle_timestamp"); row.vehicleTime = epoch(row.vehicleTimestampRaw);
        row.previousVehicleTime = epoch(longValue(v, "previous_vehicle_timestamp")); row.ingestTime = ingestTime; row.freshnessSeconds = longValue(v, "freshness_seconds");
        row.vehicleGapSeconds = longValue(v, "vehicle_gap_seconds"); row.distanceFromPreviousM = decimal(v, "distance_from_previous_m"); row.derivedSpeedKmh = decimal(v, "derived_speed_kmh");
        row.positionPresent = bool(v, "position_present"); row.positionWgs84Valid = bool(v, "position_wgs84_valid"); row.zeroZeroPosition = bool(v, "zero_zero_position");
        row.feedBoundsValid = bool(v, "feed_bounds_valid"); row.positionQcStatus = text(v, "position_qc_status"); row.gpsJumpStatus = text(v, "gps_jump_status");
        row.jumpCalculationSkippedReason = text(v, "jump_calculation_skipped_reason"); row.analysisEligible = bool(v, "analysis_eligible"); row.spatialEligible = bool(v, "spatial_eligible");
        row.duplicateObservation = requiredBoolean(v, "duplicate_observation"); row.observationKey = text(v, "observation_key");
        row.qcFlags = copy(v.get("qc_flags")); row.realtimeEntity = copy(v.get("realtime_entity"));
        if (row.qcFlags == null || !row.qcFlags.isArray()) throw new IllegalArgumentException("qc_flags must be an array at sequence " + sequence);
        if (row.realtimeEntity == null || !row.realtimeEntity.isObject()) throw new IllegalArgumentException("realtime_entity must be a complete object at sequence " + sequence);
        // 无效坐标与 (0,0) 原值仍保留，但不构造误导性的 PostGIS Geometry。
        if (validWgs84(row.latitude, row.longitude) && !zeroZero(row.latitude, row.longitude)) row.geom = PostgisGeometry.point(row.longitude, row.latitude);
        // Static 外键只从本 Feed 的解析索引解析，禁止跨 Feed 误关联。
        String routeKey = row.staticRouteId != null ? row.staticRouteId : row.resolvedRouteId;
        row.staticRouteUid = routeKey == null ? null : refs.routeUids().get(routeKey);
        row.staticTripUid = row.tripId == null ? null : refs.tripUids().get(row.tripId);
        row.staticShapeUid = row.shapeId == null ? null : refs.shapeUids().get(row.shapeId);
        row.staticStopUid = row.stopId == null ? null : refs.stopUids().get(row.stopId);
        return row;
    }

    private JbVehicleObservationQcEntity qcRow(UUID observationUid, String code, JsonNode vehicle) {
        JbVehicleObservationQcEntity row = new JbVehicleObservationQcEntity();
        row.uid = UUID.randomUUID(); row.observationUid = observationUid; row.qcCode = code;
        row.qcCategory = category(code); row.severity = severity(code);
        if ("GPS_JUMP".equals(code) || "SUSPICIOUS_SPEED".equals(code)) row.qcValueNumeric = decimal(vehicle, "derived_speed_kmh");
        if ("FUTURE_TIMESTAMP".equals(code) || "STALE_TIMESTAMP".equals(code) || "LONG_GAP".equals(code)) row.qcValueNumeric = decimal(vehicle, "freshness_seconds");
        row.details = JsonNodeFactory.instance.objectNode().put("source", "enriched_full.json").put("qc_code", code);
        return row;
    }

    private JbVehicleLatestStateEntity latestState(JbVehicleObservationEntity observation) {
        JbVehicleLatestStateEntity row = new JbVehicleLatestStateEntity();
        row.uid = UUID.randomUUID(); row.feedUid = observation.feedUid; row.observationUid = observation.uid; row.vehicleId = observation.vehicleId;
        row.vehicleLabel = observation.vehicleLabel; row.licensePlate = observation.licensePlate; row.tripId = observation.tripId; row.resolvedRouteId = observation.resolvedRouteId;
        row.routeShortName = observation.routeShortName; row.routeLongName = observation.routeLongName; row.staticDirectionId = observation.staticDirectionId;
        row.latitude = observation.latitude; row.longitude = observation.longitude; row.geom = observation.geom; row.sourceSpeedRaw = observation.sourceSpeedRaw;
        row.derivedSpeedKmh = observation.derivedSpeedKmh; row.vehicleTimestampRaw = observation.vehicleTimestampRaw; row.vehicleTime = observation.vehicleTime;
        row.lastSeenTime = observation.ingestTime; row.freshnessSeconds = observation.freshnessSeconds; row.analysisEligible = observation.analysisEligible;
        row.spatialEligible = observation.spatialEligible; row.qcFlags = observation.qcFlags.deepCopy();
        return row;
    }

    private String category(String code) {
        if (code.contains("POSITION") || code.contains("WGS84") || code.contains("BOUNDS") || code.contains("GPS") || code.contains("SPEED")) return "SPATIAL";
        if (code.contains("TIME") || code.contains("GAP") || code.contains("STALE") || code.contains("STATIONARY")) return "TEMPORAL";
        if (code.contains("ROUTE") || code.contains("TRIP")) return "STATIC_LINK";
        if (code.contains("DUPLICATE")) return "DUPLICATE";
        return "GENERAL";
    }
    private String severity(String code) { return switch (code) { case "GPS_JUMP", "FUTURE_TIMESTAMP", "INVALID_WGS84", "ZERO_ZERO_POSITION" -> "WARN"; default -> "INFO"; }; }
    private JsonNode requiredObject(JsonNode root, String field) { JsonNode value = root.get(field); if (value == null || !value.isObject()) throw new IllegalArgumentException(field + " must be an object."); return value; }
    private String text(JsonNode node, String field) { JsonNode value = node.get(field); return value == null || value.isNull() || !value.isValueNode() || value.asText().isBlank() ? null : value.asText(); }
    private Double decimal(JsonNode node, String field) { JsonNode value = node.get(field); return value == null || value.isNull() || !value.isNumber() ? null : value.doubleValue(); }
    private Integer integer(JsonNode node, String field) { JsonNode value = node.get(field); if (value == null || value.isNull()) return null; if (value.isIntegralNumber()) return value.intValue(); try { return Integer.valueOf(value.asText()); } catch (NumberFormatException ex) { return null; } }
    private Short shortInteger(JsonNode node, String field) { Integer value = integer(node, field); return value == null ? null : value.shortValue(); }
    private Long longValue(JsonNode node, String field) { JsonNode value = node.get(field); return value == null || value.isNull() || !value.isNumber() ? null : value.longValue(); }
    private Boolean bool(JsonNode node, String field) { JsonNode value = node.get(field); return value == null || value.isNull() || !value.isBoolean() ? null : value.booleanValue(); }
    private Boolean requiredBoolean(JsonNode node, String field) { Boolean value = bool(node, field); if (value == null) throw new IllegalArgumentException(field + " must be a boolean."); return value; }
    private Instant instant(JsonNode node, String field) { String value = text(node, field); return value == null ? null : Instant.parse(value); }
    private Instant epoch(Long value) { return value == null ? null : Instant.ofEpochSecond(value); }
    private Integer gtfsSeconds(String value) { try { return value == null ? null : new GtfsTimeParser().parseToSeconds(value); } catch (RuntimeException ex) { return null; } }
    private LocalDate gtfsDate(String value) { try { return value == null ? null : LocalDate.parse(value, GTFS_DATE); } catch (DateTimeParseException ex) { return null; } }
    private JsonNode copy(JsonNode value) { return value == null || value.isNull() ? null : value.deepCopy(); }
    private boolean validWgs84(Double lat, Double lon) { return lat != null && lon != null && Double.isFinite(lat) && Double.isFinite(lon) && lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180; }
    private boolean zeroZero(Double lat, Double lon) { return lat != null && lon != null && Double.compare(lat, 0.0d) == 0 && Double.compare(lon, 0.0d) == 0; }
}
