package com.mytransitgps.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.JsonNode;
import com.mytransitgps.persistence.typehandler.PostgisGeometry;
import com.mytransitgps.persistence.typehandler.PostgreSqlGeometryTypeHandler;
import com.mytransitgps.persistence.typehandler.PostgreSqlJsonbTypeHandler;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 中文名称：jb.vehicle_observation 数据库实体。
 *
 * 功能说明：严格按照已冻结的 PostgreSQL 16 实际列定义映射，
 * 仅用于 MyBatis-Plus 查询与持久化，不触发任何 Schema 自动变更。
 */
@TableName(value = "jb.vehicle_observation", autoResultMap = true)
public class JbVehicleObservationEntity {

    @TableId(value = "uid", type = IdType.INPUT)
    public UUID uid;

    @TableField("feed_uid")
    public UUID feedUid;

    @TableField("run_uid")
    public UUID runUid;

    @TableField("request_uid")
    public UUID requestUid;

    @TableField("snapshot_uid")
    public UUID snapshotUid;

    @TableField("static_version_uid")
    public UUID staticVersionUid;

    @TableField("static_route_uid")
    public UUID staticRouteUid;

    @TableField("static_trip_uid")
    public UUID staticTripUid;

    @TableField("static_shape_uid")
    public UUID staticShapeUid;

    @TableField("static_stop_uid")
    public UUID staticStopUid;

    @TableField("entity_sequence")
    public Integer entitySequence;

    @TableField("entity_id")
    public String entityId;

    @TableField("vehicle_id")
    public String vehicleId;

    @TableField("vehicle_label")
    public String vehicleLabel;

    @TableField("license_plate")
    public String licensePlate;

    @TableField("wheelchair_accessible")
    public String wheelchairAccessible;

    @TableField("trip_id")
    public String tripId;

    @TableField("trip_start_time_raw")
    public String tripStartTimeRaw;

    @TableField("trip_start_seconds")
    public Integer tripStartSeconds;

    @TableField("trip_start_date_raw")
    public String tripStartDateRaw;

    @TableField("trip_start_date")
    public LocalDate tripStartDate;

    @TableField("realtime_schedule_relationship")
    public String realtimeScheduleRelationship;

    @TableField("realtime_route_id")
    public String realtimeRouteId;

    @TableField("static_route_id")
    public String staticRouteId;

    @TableField("resolved_route_id")
    public String resolvedRouteId;

    @TableField("route_short_name")
    public String routeShortName;

    @TableField("route_long_name")
    public String routeLongName;

    @TableField("route_type")
    public Integer routeType;

    @TableField("route_color")
    public String routeColor;

    @TableField("route_text_color")
    public String routeTextColor;

    @TableField("route_resolution_method")
    public String routeResolutionMethod;

    @TableField("realtime_route_direct_matched")
    public Boolean realtimeRouteDirectMatched;

    @TableField("route_resolved")
    public Boolean routeResolved;

    @TableField("static_service_id")
    public String staticServiceId;

    @TableField("trip_headsign")
    public String tripHeadsign;

    @TableField("trip_short_name")
    public String tripShortName;

    @TableField("shape_id")
    public String shapeId;

    @TableField("trip_static_matched")
    public Boolean tripStaticMatched;

    @TableField("realtime_direction_id")
    public Short realtimeDirectionId;

    @TableField("static_direction_id")
    public Short staticDirectionId;

    @TableField("direction_comparison")
    public String directionComparison;

    @TableField("latitude")
    public Double latitude;

    @TableField("longitude")
    public Double longitude;

    @TableField(value = "geom", typeHandler = PostgreSqlGeometryTypeHandler.class)
    public PostgisGeometry geom;

    @TableField("bearing")
    public Double bearing;

    @TableField("odometer_m")
    public Double odometerM;

    @TableField("source_speed_raw")
    public Double sourceSpeedRaw;

    @TableField("source_speed_present")
    public Boolean sourceSpeedPresent;

    @TableField("source_speed_unit_declared")
    public String sourceSpeedUnitDeclared;

    @TableField("source_speed_unit_interpretation")
    public String sourceSpeedUnitInterpretation;

    @TableField("current_stop_sequence")
    public Integer currentStopSequence;

    @TableField("stop_id")
    public String stopId;

    @TableField("current_status")
    public String currentStatus;

    @TableField("congestion_level")
    public String congestionLevel;

    @TableField("occupancy_status")
    public String occupancyStatus;

    @TableField("occupancy_percentage")
    public Integer occupancyPercentage;

    @TableField(value = "multi_carriage_details", typeHandler = PostgreSqlJsonbTypeHandler.class)
    public JsonNode multiCarriageDetails;

    @TableField("vehicle_timestamp_raw")
    public Long vehicleTimestampRaw;

    @TableField("vehicle_time")
    public Instant vehicleTime;

    @TableField("previous_vehicle_time")
    public Instant previousVehicleTime;

    @TableField("ingest_time")
    public Instant ingestTime;

    @TableField("freshness_seconds")
    public Long freshnessSeconds;

    @TableField("vehicle_gap_seconds")
    public Long vehicleGapSeconds;

    @TableField("distance_from_previous_m")
    public Double distanceFromPreviousM;

    @TableField("derived_speed_kmh")
    public Double derivedSpeedKmh;

    @TableField("position_present")
    public Boolean positionPresent;

    @TableField("position_wgs84_valid")
    public Boolean positionWgs84Valid;

    @TableField("zero_zero_position")
    public Boolean zeroZeroPosition;

    @TableField("feed_bounds_valid")
    public Boolean feedBoundsValid;

    @TableField("position_qc_status")
    public String positionQcStatus;

    @TableField("gps_jump_status")
    public String gpsJumpStatus;

    @TableField("jump_calculation_skipped_reason")
    public String jumpCalculationSkippedReason;

    @TableField("analysis_eligible")
    public Boolean analysisEligible;

    @TableField("spatial_eligible")
    public Boolean spatialEligible;

    @TableField("duplicate_observation")
    public Boolean duplicateObservation;

    @TableField("observation_key")
    public String observationKey;

    @TableField(value = "qc_flags", typeHandler = PostgreSqlJsonbTypeHandler.class)
    public JsonNode qcFlags;

    @TableField(value = "realtime_entity", typeHandler = PostgreSqlJsonbTypeHandler.class)
    public JsonNode realtimeEntity;

    @TableField("create_time")
    public Instant createTime;

}
