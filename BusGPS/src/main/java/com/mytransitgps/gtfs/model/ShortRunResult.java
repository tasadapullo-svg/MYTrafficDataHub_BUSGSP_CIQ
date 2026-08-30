package com.mytransitgps.gtfs.model;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 短时采集运行结果，汇总周期证据、统计指标和报告路径。
 */
public record ShortRunResult(
        String runId,
        LocalDate batchDate,
        Instant runStartedAt,
        Instant runFinishedAt,
        int plannedCycles,
        int completedCycles,
        int plannedRealtimeRequests,
        int actualRealtimeRequests,
        int successfulRequests,
        int failedRequests,
        int http429Count,
        int http5xxCount,
        int timeoutCount,
        List<Map<String, Object>> feedSummaries,
        List<Map<String, Object>> snapshotComparisons,
        Path manifestPath,
        Path feedSummaryCsvPath,
        Path snapshotComparisonCsvPath,
        Path fieldPresenceJsonPath,
        Path fieldPresenceCsvPath,
        Path runReportRoot) {
}
