package com.mytransitgps.persistence.model;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

/**
 * 中文名称：Johor Bahru 单轮采集证据。
 *
 * 功能说明：固定记录一次真实请求对应的数据库主键、三类磁盘文件、调度时间和持久化性能，
 * 供采集结束后的独立磁盘重读验证使用。
 */
public record JohorCycleEvidence(
        int cycle,
        UUID requestUid,
        UUID snapshotUid,
        Instant scheduledAt,
        long schedulerDriftMs,
        int httpStatus,
        Path rawPath,
        Path parsedPath,
        Path enrichedPath,
        String responseSha256,
        int entityCount,
        int vehicleCount,
        SnapshotPersistenceResult persistenceResult) {
}
