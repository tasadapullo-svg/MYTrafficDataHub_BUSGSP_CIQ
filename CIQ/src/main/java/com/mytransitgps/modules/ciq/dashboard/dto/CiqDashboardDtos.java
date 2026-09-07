package com.mytransitgps.modules.ciq.dashboard.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** CIQ 大屏只读 JSON DTO。 */
public final class CiqDashboardDtos {
    private CiqDashboardDtos() { }

    public record Overview(long plannedTasks, long executedTasks, long successfulTasks, long failedTasks,
                           Double successRate, Instant generatedAt) { }

    public record ApiStatus(String apiCode, String apiName, String nameZh, String endpoint, String schedule,
                            String status, Instant lastRequest, Instant lastSuccess, Instant lastSuccessTime, Instant latestDataTime,
                            Long todayCount, Long totalCount, Integer httpStatus,
                            Long rawRecords, Long ciqSelected, Long dbInserted, long todayExecutions,
                            String completeness, Integer retryCount, Integer pages, Long responseBytes,
                            Long durationMs, String lastError, String rawFile, Long fileSizeBytes,
                            String sha256, Map<String, Object> metrics) { }

    public record TrendPoint(String apiCode, Instant bucketTime, long executions, long records) { }

    public record AuditCell(String text, String status) { }
    public record AuditRow(String code, String itemEn, String itemZh, Map<String, AuditCell> apis) { }

    public record RequestLog(Instant time, String apiCode, String apiName, Integer httpStatus,
                             Long rawRecords, Long ciqSelected, Long inserted, Long durationMs,
                             Integer retry, String archive, String status) { }

    public record MapPoint(String apiCode, String recordId, String title, String category,
                           double latitude, double longitude, Instant observedAt,
                           Map<String, Object> details) { }

    public record DataRecord(String apiCode, String recordId, String category, String title,
                             String description, Instant eventTime, String status,
                             Double latitude, Double longitude, Map<String, Object> details) { }

    public record Storage(long rawTodayFiles, long rawTotalFiles, long rawTodayBytes, long rawTotalBytes,
                          long archiveCount, Instant latestArchive, String archiveStatus,
                          long dbTodayInserted, long dbTotalRows, Instant latestDbInsert, String dbStatus,
                          Long diskTotalBytes, Long diskUsedBytes, Long diskFreeBytes, Double diskUsagePercent,
                          Instant generatedAt) { }

    public record DashboardPayload(Overview overview, List<ApiStatus> apis, List<TrendPoint> trend,
                                   List<AuditRow> audit, List<RequestLog> requests, Storage storage) { }
}
