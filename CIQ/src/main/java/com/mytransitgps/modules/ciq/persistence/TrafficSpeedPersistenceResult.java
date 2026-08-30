package com.mytransitgps.modules.ciq.persistence;

/**
 * API01 单页数据库写入统计。
 *
 * <p>用于 collector 汇总 Link、Scope、Observation 的新增、更新、重复和区域外数量。</p>
 */
public record TrafficSpeedPersistenceResult(
        long newLinkCount,
        long updatedLinkCount,
        long scopeRows,
        long observationsInserted,
        long observationDuplicates,
        long outOfScopeCount,
        long uniqueInScopeLinks
) {
    public static TrafficSpeedPersistenceResult empty() {
        return new TrafficSpeedPersistenceResult(0, 0, 0, 0, 0, 0, 0);
    }

    public TrafficSpeedPersistenceResult plus(TrafficSpeedPersistenceResult other) {
        return new TrafficSpeedPersistenceResult(
                newLinkCount + other.newLinkCount,
                updatedLinkCount + other.updatedLinkCount,
                scopeRows + other.scopeRows,
                observationsInserted + other.observationsInserted,
                observationDuplicates + other.observationDuplicates,
                outOfScopeCount + other.outOfScopeCount,
                uniqueInScopeLinks + other.uniqueInScopeLinks
        );
    }
}

