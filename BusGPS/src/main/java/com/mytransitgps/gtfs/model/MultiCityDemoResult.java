package com.mytransitgps.gtfs.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.nio.file.Path;

/**
 * 多城市演示采集的整体结果，汇总各 Feed 结果与输出目录。
 */
public record MultiCityDemoResult(
        String batchId,
        LocalDate batchDate,
        Instant startedAt,
        Instant finishedAt,
        List<MultiCityFeedRunResult> feedResults,
        Path fieldPresenceMatrixJsonPath,
        Path fieldPresenceMatrixCsvPath) {
}
