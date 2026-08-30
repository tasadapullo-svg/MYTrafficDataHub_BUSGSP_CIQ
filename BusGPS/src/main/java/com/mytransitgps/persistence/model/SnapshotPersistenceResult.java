package com.mytransitgps.persistence.model;

import java.util.UUID;

/**
 * 中文名称：Realtime Snapshot 持久化结果。
 *
 * 功能说明：记录事务提交后的 Snapshot、Observation、QC、Latest State 数量和耗时。
 */
public record SnapshotPersistenceResult(
        UUID snapshotUid,
        int observationCount,
        int qcCount,
        int latestStateUpsertCount,
        long persistenceElapsedMs) {
}
