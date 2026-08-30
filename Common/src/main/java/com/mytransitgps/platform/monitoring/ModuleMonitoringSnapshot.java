package com.mytransitgps.platform.monitoring;

import com.mytransitgps.platform.collection.ModuleCode;
import java.time.Instant;

/** 各交通模块面向统一Dashboard的最小只读快照。 */
public record ModuleMonitoringSnapshot(
        ModuleCode moduleCode,
        String moduleName,
        String status,
        Instant lastCollectionTime,
        Long todayRecords,
        Long yesterdayRecords,
        Long requestCount,
        Long successCount,
        Long failureCount,
        Instant latestDataTime
) {
    public static ModuleMonitoringSnapshot unavailable(ModuleCode code, String name, String status) {
        return new ModuleMonitoringSnapshot(code, name, status, null, null, null, null, null, null, null);
    }
}
