package com.mytransitgps.persistence.model;

import com.mytransitgps.gtfs.model.FeedSpatialBounds;
import com.mytransitgps.gtfs.model.StaticFeedData;

/**
 * 中文名称：数据库采集所需 Static 准备结果。
 *
 * 功能说明：组合经过既有解析器解析的 Static 业务数据、空间边界、
 * 本地 ZIP 证据和数据库 UUID 索引，供计划轮次 Realtime 采集全程复用。
 */
public record PreparedDatabaseStatic(
        LocalStaticArtifact artifact,
        StaticFeedData feedData,
        FeedSpatialBounds bounds,
        StaticReferenceIndex references,
        boolean existingVersionReused,
        boolean newVersionImported) {
}
