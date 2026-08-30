package com.mytransitgps.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import java.util.UUID;

/**
 * 中文名称：jb.realtime_snapshot 数据库实体。
 *
 * 功能说明：严格按照已冻结的 PostgreSQL 16 实际列定义映射，
 * 仅用于 MyBatis-Plus 查询与持久化，不触发任何 Schema 自动变更。
 */
@TableName(value = "jb.realtime_snapshot", autoResultMap = true)
public class JbRealtimeSnapshotEntity {

    @TableId(value = "uid", type = IdType.INPUT)
    public UUID uid;

    @TableField("feed_uid")
    public UUID feedUid;

    @TableField("run_uid")
    public UUID runUid;

    @TableField("request_uid")
    public UUID requestUid;

    @TableField("enrichment_static_version_uid")
    public UUID enrichmentStaticVersionUid;

    @TableField("batch_id")
    public String batchId;

    @TableField("gtfs_realtime_version")
    public String gtfsRealtimeVersion;

    @TableField("incrementality")
    public String incrementality;

    @TableField("feed_timestamp_raw")
    public Long feedTimestampRaw;

    @TableField("feed_time")
    public Instant feedTime;

    @TableField("entity_count")
    public Integer entityCount;

    @TableField("vehicle_count")
    public Integer vehicleCount;

    @TableField("unique_route_count")
    public Integer uniqueRouteCount;

    @TableField("route_direct_match_count")
    public Integer routeDirectMatchCount;

    @TableField("route_trip_fallback_match_count")
    public Integer routeTripFallbackMatchCount;

    @TableField("route_resolved_count")
    public Integer routeResolvedCount;

    @TableField("route_unresolved_count")
    public Integer routeUnresolvedCount;

    @TableField("trip_match_count")
    public Integer tripMatchCount;

    @TableField("trip_unmatched_count")
    public Integer tripUnmatchedCount;

    @TableField("direction_match_count")
    public Integer directionMatchCount;

    @TableField("direction_mismatch_count")
    public Integer directionMismatchCount;

    @TableField("direction_not_comparable_count")
    public Integer directionNotComparableCount;

    @TableField("response_sha256")
    public String responseSha256;

    @TableField("duplicate_snapshot")
    public Boolean duplicateSnapshot;

    @TableField("referenced_snapshot_uid")
    public UUID referencedSnapshotUid;

    @TableField("create_time")
    public Instant createTime;

}
