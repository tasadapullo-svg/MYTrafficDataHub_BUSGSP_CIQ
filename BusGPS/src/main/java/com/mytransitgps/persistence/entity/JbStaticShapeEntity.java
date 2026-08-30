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
 * 中文名称：jb.static_shape 数据库实体。
 *
 * 功能说明：严格按照已冻结的 PostgreSQL 16 实际列定义映射，
 * 仅用于 MyBatis-Plus 查询与持久化，不触发任何 Schema 自动变更。
 */
@TableName(value = "jb.static_shape", autoResultMap = true)
public class JbStaticShapeEntity {

    @TableId(value = "uid", type = IdType.INPUT)
    public UUID uid;

    @TableField("static_version_uid")
    public UUID staticVersionUid;

    @TableField("shape_id")
    public String shapeId;

    @TableField("is_referenced")
    public Boolean isReferenced;

    @TableField("trip_count_using_shape")
    public Integer tripCountUsingShape;

    @TableField("point_count")
    public Integer pointCount;

    @TableField(value = "geom", typeHandler = PostgreSqlGeometryTypeHandler.class)
    public PostgisGeometry geom;

    @TableField("shape_length_m")
    public Double shapeLengthM;

    @TableField("shape_length_km")
    public Double shapeLengthKm;

    @TableField("analysis_eligible")
    public Boolean analysisEligible;

    @TableField(value = "qc_flags", typeHandler = PostgreSqlJsonbTypeHandler.class)
    public JsonNode qcFlags;

    @TableField(value = "qc_summary", typeHandler = PostgreSqlJsonbTypeHandler.class)
    public JsonNode qcSummary;

    @TableField("create_time")
    public Instant createTime;

}
