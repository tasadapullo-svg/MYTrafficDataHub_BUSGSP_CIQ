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
 * 中文名称：jb.static_shape_point 数据库实体。
 *
 * 功能说明：严格按照已冻结的 PostgreSQL 16 实际列定义映射，
 * 仅用于 MyBatis-Plus 查询与持久化，不触发任何 Schema 自动变更。
 */
@TableName(value = "jb.static_shape_point", autoResultMap = true)
public class JbStaticShapePointEntity {

    @TableId(value = "uid", type = IdType.INPUT)
    public UUID uid;

    @TableField("static_version_uid")
    public UUID staticVersionUid;

    @TableField("shape_uid")
    public UUID shapeUid;

    @TableField("shape_id")
    public String shapeId;

    @TableField("shape_pt_lat")
    public Double shapePtLat;

    @TableField("shape_pt_lon")
    public Double shapePtLon;

    @TableField(value = "geom", typeHandler = PostgreSqlGeometryTypeHandler.class)
    public PostgisGeometry geom;

    @TableField("shape_pt_sequence")
    public Integer shapePtSequence;

    @TableField("shape_dist_traveled")
    public Double shapeDistTraveled;

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
