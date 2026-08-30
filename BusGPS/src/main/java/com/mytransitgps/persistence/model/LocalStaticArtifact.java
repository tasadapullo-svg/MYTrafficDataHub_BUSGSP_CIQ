package com.mytransitgps.persistence.model;

import java.nio.file.Path;
import java.time.Instant;

/**
 * 中文名称：本地 GTFS Static 证据对象。
 *
 * 功能说明：指向已落盘的 Static ZIP 及其元数据，供数据库版本复用或一次性导入使用。
 */
public record LocalStaticArtifact(
        Path zipPath,
        Path metadataPath,
        String sha256,
        long zipSizeBytes,
        Instant downloadedAt) {
}
