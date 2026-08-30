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
 * 中文名称：jb.static_trip 数据库实体。
 *
 * 功能说明：严格按照已冻结的 PostgreSQL 16 实际列定义映射，
 * 仅用于 MyBatis-Plus 查询与持久化，不触发任何 Schema 自动变更。
 */
@TableName(value = "jb.static_trip", autoResultMap = true)
public class JbStaticTripEntity {

    @TableId(value = "uid", type = IdType.INPUT)
    public UUID uid;

    @TableField("static_version_uid")
    public UUID staticVersionUid;

    @TableField("route_uid")
    public UUID routeUid;

    @TableField("shape_uid")
    public UUID shapeUid;

    @TableField("route_id")
    public String routeId;

    @TableField("service_id")
    public String serviceId;

    @TableField("trip_id")
    public String tripId;

    @TableField("trip_headsign")
    public String tripHeadsign;

    @TableField("trip_short_name")
    public String tripShortName;

    @TableField("direction_id")
    public Short directionId;

    @TableField("block_id")
    public String blockId;

    @TableField("shape_id")
    public String shapeId;

    @TableField("wheelchair_accessible")
    public Integer wheelchairAccessible;

    @TableField("bikes_allowed")
    public Integer bikesAllowed;

    @TableField(value = "source_row", typeHandler = PostgreSqlJsonbTypeHandler.class)
    public JsonNode sourceRow;

    @TableField("create_time")
    public Instant createTime;

}
