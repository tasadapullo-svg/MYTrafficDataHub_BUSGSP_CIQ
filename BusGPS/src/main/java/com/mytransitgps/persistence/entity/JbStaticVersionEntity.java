package com.mytransitgps.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import java.util.UUID;

/**
 * 中文名称：jb.static_version 数据库实体。
 *
 * 功能说明：严格按照已冻结的 PostgreSQL 16 实际列定义映射，
 * 仅用于 MyBatis-Plus 查询与持久化，不触发任何 Schema 自动变更。
 */
@TableName(value = "jb.static_version", autoResultMap = true)
public class JbStaticVersionEntity {

    @TableId(value = "uid", type = IdType.INPUT)
    public UUID uid;

    @TableField("feed_uid")
    public UUID feedUid;

    @TableField("source_request_uid")
    public UUID sourceRequestUid;

    @TableField("version_code")
    public String versionCode;

    @TableField("static_sha256")
    public String staticSha256;

    @TableField("static_object_path")
    public String staticObjectPath;

    @TableField("zip_size_bytes")
    public Long zipSizeBytes;

    @TableField("downloaded_at")
    public Instant downloadedAt;

    @TableField("effective_from")
    public Instant effectiveFrom;

    @TableField("effective_to")
    public Instant effectiveTo;

    @TableField("is_current")
    public Boolean isCurrent;

    @TableField("routes_file_present")
    public Boolean routesFilePresent;

    @TableField("trips_file_present")
    public Boolean tripsFilePresent;

    @TableField("stops_file_present")
    public Boolean stopsFilePresent;

    @TableField("stop_times_file_present")
    public Boolean stopTimesFilePresent;

    @TableField("shapes_file_present")
    public Boolean shapesFilePresent;

    @TableField("calendar_file_present")
    public Boolean calendarFilePresent;

    @TableField("calendar_dates_file_present")
    public Boolean calendarDatesFilePresent;

    @TableField("frequencies_file_present")
    public Boolean frequenciesFilePresent;

    @TableField("routes_count")
    public Integer routesCount;

    @TableField("trips_count")
    public Integer tripsCount;

    @TableField("stops_count")
    public Integer stopsCount;

    @TableField("stop_times_count")
    public Integer stopTimesCount;

    @TableField("shapes_count")
    public Integer shapesCount;

    @TableField("shape_points_count")
    public Integer shapePointsCount;

    @TableField("notes")
    public String notes;

    @TableField("create_time")
    public Instant createTime;

}
