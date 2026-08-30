package com.mytransitgps.modules.ciq.storage;

import java.nio.file.Path;
import java.time.LocalDate;

/**
 * 单个 CIQ 原始 JSON 文件的落盘证据。
 *
 * <p>用于记录接口代码、业务日期、分页序号、文件路径、字节数与 SHA-256，便于后续科研审计和归档校验。
 */
public record CiqRawJsonArtifact(
        CiqApiCode apiCode,
        LocalDate dataDate,
        int pageNo,
        Path path,
        long sizeBytes,
        String sha256
) {
}
