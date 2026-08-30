package com.mytransitgps.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import java.util.UUID;

/**
 * 中文名称：core.gtfs_feed 数据库实体。
 *
 * 功能说明：严格按照已冻结的 PostgreSQL 16 实际列定义映射，
 * 仅用于 MyBatis-Plus 查询与持久化，不触发任何 Schema 自动变更。
 */
@TableName(value = "core.gtfs_feed", autoResultMap = true)
public class CoreGtfsFeedEntity {

    @TableId(value = "uid", type = IdType.INPUT)
    public UUID uid;

    @TableField("city_uid")
    public UUID cityUid;

    @TableField("feed_id")
    public String feedId;

    @TableField("feed_name")
    public String feedName;

    @TableField("operator_name")
    public String operatorName;

    @TableField("service_type")
    public String serviceType;

    @TableField("realtime_url")
    public String realtimeUrl;

    @TableField("static_url")
    public String staticUrl;

    @TableField("realtime_feed_type")
    public String realtimeFeedType;

    @TableField("source_platform")
    public String sourcePlatform;

    @TableField("file_prefix")
    public String filePrefix;

    @TableField("poll_interval_seconds")
    public Integer pollIntervalSeconds;

    @TableField("stagger_offset_seconds")
    public Integer staggerOffsetSeconds;

    @TableField("timezone_name")
    public String timezoneName;

    @TableField("enabled")
    public Boolean enabled;

    @TableField("description")
    public String description;

    @TableField("create_time")
    public Instant createTime;

    @TableField("update_time")
    public Instant updateTime;

}
