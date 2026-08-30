package com.mytransitgps.gtfs.model;

import java.util.List;
import java.util.Map;

/**
 * 计划班次时间校验结果，记录可比较数量和时间偏差统计。
 */
public record ScheduledTripTimeValidationResult(
        Map<String, Object> summary,
        List<Map<String, Object>> tripRows,
        List<Map<String, Object>> routeSummaryRows) {
}
