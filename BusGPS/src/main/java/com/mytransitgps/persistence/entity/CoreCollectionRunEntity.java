package com.mytransitgps.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import java.util.UUID;

/**
 * 中文名称：core.collection_run 数据库实体。
 *
 * 功能说明：严格按照已冻结的 PostgreSQL 16 实际列定义映射，
 * 仅用于 MyBatis-Plus 查询与持久化，不触发任何 Schema 自动变更。
 */
@TableName(value = "core.collection_run", autoResultMap = true)
public class CoreCollectionRunEntity {

    @TableId(value = "uid", type = IdType.INPUT)
    public UUID uid;

    @TableField("run_code")
    public String runCode;

    @TableField("run_type")
    public String runType;

    @TableField("run_status")
    public String runStatus;

    @TableField("timezone_name")
    public String timezoneName;

    @TableField("planned_start_time")
    public Instant plannedStartTime;

    @TableField("actual_start_time")
    public Instant actualStartTime;

    @TableField("actual_end_time")
    public Instant actualEndTime;

    @TableField("planned_cycle_count")
    public Integer plannedCycleCount;

    @TableField("actual_cycle_count")
    public Integer actualCycleCount;

    @TableField("feed_count")
    public Integer feedCount;

    @TableField("planned_request_count")
    public Integer plannedRequestCount;

    @TableField("actual_request_count")
    public Integer actualRequestCount;

    @TableField("successful_request_count")
    public Integer successfulRequestCount;

    @TableField("failed_request_count")
    public Integer failedRequestCount;

    @TableField("http_429_count")
    public Integer http429Count;

    @TableField("http_5xx_count")
    public Integer http5xxCount;

    @TableField("timeout_count")
    public Integer timeoutCount;

    @TableField("scheduler_drift_p50_ms")
    public Double schedulerDriftP50Ms;

    @TableField("scheduler_drift_p95_ms")
    public Double schedulerDriftP95Ms;

    @TableField("scheduler_drift_max_ms")
    public Double schedulerDriftMaxMs;

    @TableField("remarks")
    public String remarks;

    @TableField("create_time")
    public Instant createTime;

    @TableField("update_time")
    public Instant updateTime;

}
