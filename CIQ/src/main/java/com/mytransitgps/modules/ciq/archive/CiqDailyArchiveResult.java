package com.mytransitgps.modules.ciq.archive;

import java.nio.file.Path;
import java.time.LocalDate;

/**
 * CIQ 每日 JSON 归档结果。
 *
 * <p>记录归档日期、ZIP、SHA-256、源文件数量、归档字节数、删除数量与最终状态。
 */
public record CiqDailyArchiveResult(
        LocalDate archiveDate,
        Path archivePath,
        String archiveSha256,
        long archiveBytes,
        int sourceFileCount,
        int deletedFileCount,
        CiqDailyArchiveStatus status,
        String errorMessage
) {
}
