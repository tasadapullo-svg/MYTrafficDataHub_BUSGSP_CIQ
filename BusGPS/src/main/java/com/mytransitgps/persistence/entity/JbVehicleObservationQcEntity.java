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
 * 中文名称：jb.vehicle_observation_qc 数据库实体。
 *
 * 功能说明：严格按照已冻结的 PostgreSQL 16 实际列定义映射，
 * 仅用于 MyBatis-Plus 查询与持久化，不触发任何 Schema 自动变更。
 */
@TableName(value = "jb.vehicle_observation_qc", autoResultMap = true)
public class JbVehicleObservationQcEntity {

    @TableId(value = "uid", type = IdType.INPUT)
    public UUID uid;

    @TableField("observation_uid")
    public UUID observationUid;

    @TableField("qc_code")
    public String qcCode;

    @TableField("qc_category")
    public String qcCategory;

    @TableField("severity")
    public String severity;

    @TableField("qc_value_numeric")
    public Double qcValueNumeric;

    @TableField("qc_value_text")
    public String qcValueText;

    @TableField(value = "details", typeHandler = PostgreSqlJsonbTypeHandler.class)
    public JsonNode details;

    @TableField("create_time")
    public Instant createTime;

}
