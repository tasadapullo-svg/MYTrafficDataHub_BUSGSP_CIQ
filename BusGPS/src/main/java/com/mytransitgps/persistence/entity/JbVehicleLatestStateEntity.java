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
import java.util.UUID;

/**
 * 中文名称：jb.vehicle_latest_state 数据库实体。
 *
 * 功能说明：严格按照已冻结的 PostgreSQL 16 实际列定义映射，
 * 仅用于 MyBatis-Plus 查询与持久化，不触发任何 Schema 自动变更。
 */
@TableName(value = "jb.vehicle_latest_state", autoResultMap = true)
public class JbVehicleLatestStateEntity {

    @TableId(value = "uid", type = IdType.INPUT)
    public UUID uid;

    @TableField("feed_uid")
    public UUID feedUid;

    @TableField("observation_uid")
    public UUID observationUid;

    @TableField("vehicle_id")
    public String vehicleId;

    @TableField("vehicle_label")
    public String vehicleLabel;

    @TableField("license_plate")
    public String licensePlate;

    @TableField("trip_id")
    public String tripId;

    @TableField("resolved_route_id")
    public String resolvedRouteId;

    @TableField("route_short_name")
    public String routeShortName;

    @TableField("route_long_name")
    public String routeLongName;

    @TableField("static_direction_id")
    public Short staticDirectionId;

    @TableField("latitude")
    public Double latitude;

    @TableField("longitude")
    public Double longitude;

    @TableField(value = "geom", typeHandler = PostgreSqlGeometryTypeHandler.class)
    public PostgisGeometry geom;

    @TableField("source_speed_raw")
    public Double sourceSpeedRaw;

    @TableField("derived_speed_kmh")
    public Double derivedSpeedKmh;

    @TableField("vehicle_timestamp_raw")
    public Long vehicleTimestampRaw;

    @TableField("vehicle_time")
    public Instant vehicleTime;

    @TableField("last_seen_time")
    public Instant lastSeenTime;

    @TableField("freshness_seconds")
    public Long freshnessSeconds;

    @TableField("analysis_eligible")
    public Boolean analysisEligible;

    @TableField("spatial_eligible")
    public Boolean spatialEligible;

    @TableField(value = "qc_flags", typeHandler = PostgreSqlJsonbTypeHandler.class)
    public JsonNode qcFlags;

    @TableField("create_time")
    public Instant createTime;

    @TableField("update_time")
    public Instant updateTime;

}
