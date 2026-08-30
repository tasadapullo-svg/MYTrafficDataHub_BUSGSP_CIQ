package com.mytransitgps.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import java.util.UUID;

/**
 * 中文名称：jb.api_request_log 数据库实体。
 *
 * 功能说明：严格按照已冻结的 PostgreSQL 16 实际列定义映射，
 * 仅用于 MyBatis-Plus 查询与持久化，不触发任何 Schema 自动变更。
 */
@TableName(value = "jb.api_request_log", autoResultMap = true)
public class JbApiRequestLogEntity {

    @TableId(value = "uid", type = IdType.INPUT)
    public UUID uid;

    @TableField("feed_uid")
    public UUID feedUid;

    @TableField("run_uid")
    public UUID runUid;

    @TableField("duplicate_of_request_uid")
    public UUID duplicateOfRequestUid;

    @TableField("request_type")
    public String requestType;

    @TableField("request_sequence")
    public Integer requestSequence;

    @TableField("cycle_number")
    public Integer cycleNumber;

    @TableField("requested_url")
    public String requestedUrl;

    @TableField("final_url")
    public String finalUrl;

    @TableField("scheduled_at")
    public Instant scheduledAt;

    @TableField("request_started_at")
    public Instant requestStartedAt;

    @TableField("response_received_at")
    public Instant responseReceivedAt;

    @TableField("latency_ms")
    public Long latencyMs;

    @TableField("scheduler_drift_ms")
    public Long schedulerDriftMs;

    @TableField("http_status")
    public Integer httpStatus;

    @TableField("content_type")
    public String contentType;

    @TableField("response_bytes")
    public Long responseBytes;

    @TableField("response_sha256")
    public String responseSha256;

    @TableField("redirect_count")
    public Integer redirectCount;

    @TableField("parse_status")
    public String parseStatus;

    @TableField("entity_count")
    public Integer entityCount;

    @TableField("vehicle_count")
    public Integer vehicleCount;

    @TableField("duplicate_snapshot")
    public Boolean duplicateSnapshot;

    @TableField("raw_object_path")
    public String rawObjectPath;

    @TableField("parsed_json_path")
    public String parsedJsonPath;

    @TableField("enriched_json_path")
    public String enrichedJsonPath;

    @TableField("result")
    public String result;

    @TableField("error_class")
    public String errorClass;

    @TableField("error_message")
    public String errorMessage;

    @TableField("create_time")
    public Instant createTime;

}
