package com.mytransitgps.persistence.model;

/**
 * 中文名称：单 Snapshot QC 汇总。
 *
 * 功能说明：用于汇总日志和最终性能/质量报告，不参与过滤任何车辆观测。
 */
public record SnapshotQcSummary(
        int vehicleCount,
        int qcFlagCount,
        int duplicateCount,
        int staticMatchedCount,
        int anomalyVehicleCount) {
}
