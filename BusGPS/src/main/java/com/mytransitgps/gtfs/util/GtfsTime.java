package com.mytransitgps.gtfs.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * GTFS 时间工具，统一马来西亚业务时区、批次日期和时间格式。
 */
public final class GtfsTime {

    public static final ZoneId MALAYSIA_ZONE = ZoneId.of("Asia/Kuala_Lumpur");
    public static final DateTimeFormatter BATCH_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final DateTimeFormatter FILE_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(MALAYSIA_ZONE);

    private GtfsTime() {
    }

    public static LocalDate malaysiaToday() {
        return LocalDate.now(MALAYSIA_ZONE);
    }

    public static String formatBatchDate(LocalDate batchDate) {
        return BATCH_DATE_FORMATTER.format(batchDate);
    }

    public static String formatFileTimestamp(Instant instant) {
        return FILE_TIMESTAMP_FORMATTER.format(instant);
    }
}
