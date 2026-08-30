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
 * 中文名称：jb.static_stop 数据库实体。
 *
 * 功能说明：严格按照已冻结的 PostgreSQL 16 实际列定义映射，
 * 仅用于 MyBatis-Plus 查询与持久化，不触发任何 Schema 自动变更。
 */
@TableName(value = "jb.static_stop", autoResultMap = true)
public class JbStaticStopEntity {

    @TableId(value = "uid", type = IdType.INPUT)
    public UUID uid;

    @TableField("static_version_uid")
    public UUID staticVersionUid;

    @TableField("stop_id")
    public String stopId;

    @TableField("stop_code")
    public String stopCode;

    @TableField("stop_name")
    public String stopName;

    @TableField("tts_stop_name")
    public String ttsStopName;

    @TableField("stop_desc")
    public String stopDesc;

    @TableField("stop_lat")
    public Double stopLat;

    @TableField("stop_lon")
    public Double stopLon;

    @TableField(value = "geom", typeHandler = PostgreSqlGeometryTypeHandler.class)
    public PostgisGeometry geom;

    @TableField("zone_id")
    public String zoneId;

    @TableField("stop_url")
    public String stopUrl;

    @TableField("location_type")
    public Integer locationType;

    @TableField("parent_station")
    public String parentStation;

    @TableField("stop_timezone")
    public String stopTimezone;

    @TableField("wheelchair_boarding")
    public Integer wheelchairBoarding;

    @TableField("level_id")
    public String levelId;

    @TableField("platform_code")
    public String platformCode;

    @TableField("position_present")
    public Boolean positionPresent;

    @TableField("position_wgs84_valid")
    public Boolean positionWgs84Valid;

    @TableField("zero_zero_position")
    public Boolean zeroZeroPosition;

    @TableField("spatial_eligible")
    public Boolean spatialEligible;

    @TableField(value = "qc_flags", typeHandler = PostgreSqlJsonbTypeHandler.class)
    public JsonNode qcFlags;

    @TableField(value = "source_row", typeHandler = PostgreSqlJsonbTypeHandler.class)
    public JsonNode sourceRow;

    @TableField("create_time")
    public Instant createTime;

}
