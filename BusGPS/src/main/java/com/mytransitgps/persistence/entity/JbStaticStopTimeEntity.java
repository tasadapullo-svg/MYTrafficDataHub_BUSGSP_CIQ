package com.mytransitgps.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.JsonNode;
import com.mytransitgps.persistence.typehandler.PostgreSqlJsonbTypeHandler;
import java.time.Instant;
import java.util.UUID;

/**
 * 中文名称：jb.static_stop_time 数据库实体。
 *
 * 功能说明：严格按照已冻结的 PostgreSQL 16 实际列定义映射，
 * 仅用于 MyBatis-Plus 查询与持久化，不触发任何 Schema 自动变更。
 */
@TableName(value = "jb.static_stop_time", autoResultMap = true)
public class JbStaticStopTimeEntity {

    @TableId(value = "uid", type = IdType.INPUT)
    public UUID uid;

    @TableField("static_version_uid")
    public UUID staticVersionUid;

    @TableField("trip_uid")
    public UUID tripUid;

    @TableField("stop_uid")
    public UUID stopUid;

    @TableField("trip_id")
    public String tripId;

    @TableField("stop_id")
    public String stopId;

    @TableField("stop_sequence")
    public Integer stopSequence;

    @TableField("arrival_time_raw")
    public String arrivalTimeRaw;

    @TableField("arrival_seconds")
    public Integer arrivalSeconds;

    @TableField("departure_time_raw")
    public String departureTimeRaw;

    @TableField("departure_seconds")
    public Integer departureSeconds;

    @TableField("stop_headsign")
    public String stopHeadsign;

    @TableField("pickup_type")
    public Integer pickupType;

    @TableField("drop_off_type")
    public Integer dropOffType;

    @TableField("continuous_pickup")
    public Integer continuousPickup;

    @TableField("continuous_drop_off")
    public Integer continuousDropOff;

    @TableField("shape_dist_traveled")
    public Double shapeDistTraveled;

    @TableField("timepoint")
    public Integer timepoint;

    @TableField(value = "source_row", typeHandler = PostgreSqlJsonbTypeHandler.class)
    public JsonNode sourceRow;

    @TableField("create_time")
    public Instant createTime;

}
