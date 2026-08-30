package com.mytransitgps.persistence.model;

import java.util.UUID;

/**
 * 中文名称：Realtime Snapshot 数据库事务上下文。
 *
 * 功能说明：携带一次已落盘并重读 JSON 对应的数据库主外键及重复快照关系。
 */
public record SnapshotPersistenceContext(
        UUID feedUid,
        UUID runUid,
        UUID requestUid,
        UUID snapshotUid,
        UUID referencedSnapshotUid,
        boolean duplicateSnapshot,
        StaticReferenceIndex staticReferences) {
}
