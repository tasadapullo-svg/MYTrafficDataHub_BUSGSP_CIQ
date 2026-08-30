package com.mytransitgps.persistence.model;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

/**
 * 中文名称：多城市单次请求证据。
 *
 * 功能：固定记录一个 Feed 的一个 Cycle 对应的路由、文件、HTTP 和数据库结果；
 * 输入来自真实采集，输出给离线验证器；失败证据同样保留。
 */
public record MulticityCycleEvidence(
        String city,
        String schema,
        String feedId,
        UUID feedUid,
        int cycle,
        int requestSequence,
        UUID requestUid,
        UUID snapshotUid,
        Instant scheduledAt,
        long schedulerDriftMs,
        int httpStatus,
        String errorClass,
        Path rawPath,
        Path parsedPath,
        Path enrichedPath,
        String responseSha256,
        int entityCount,
        int vehicleCount,
        SnapshotPersistenceResult persistenceResult) {
}
