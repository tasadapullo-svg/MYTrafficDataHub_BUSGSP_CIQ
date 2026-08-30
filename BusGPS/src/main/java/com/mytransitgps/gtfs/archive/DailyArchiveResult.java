package com.mytransitgps.gtfs.archive;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/**
 * 单日归档执行结果，记录源文件、ZIP 条目、校验值、状态及告警信息。
 */
public record DailyArchiveResult(
        LocalDate archiveDate,
        Path archivePath,
        String archiveSha256,
        long archiveBytes,
        int realtimeMetadataCount,
        int staticMetadataCount,
        int parsedJsonCount,
        int enrichedJsonCount,
        int rawPbCount,
        int staticObjectCount,
        int runReportFileCount,
        int sourceFileCount,
        int archiveEntryCount,
        DailyArchiveStatus status,
        List<String> warningMessages,
        String errorMessage) {
}
