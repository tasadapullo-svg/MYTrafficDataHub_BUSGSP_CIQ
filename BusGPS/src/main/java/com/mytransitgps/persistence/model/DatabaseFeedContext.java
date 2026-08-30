package com.mytransitgps.persistence.model;

import java.util.UUID;

/**
 * 中文名称：多城市 Feed 数据库上下文。
 *
 * 功能：组合 core.gtfs_feed 与 core.study_city 的实际配置；输入为数据库记录，
 * 输出给调度、文件归档、Static 和 Schema 路由；所有 UUID 均从数据库解析，绝不硬编码。
 */
public record DatabaseFeedContext(
        UUID feedUid,
        String feedId,
        UUID cityUid,
        String cityCode,
        String cityName,
        String schemaName,
        String realtimeUrl,
        String staticUrl,
        String filePrefix,
        int pollIntervalSeconds,
        int staggerOffsetSeconds) {

    public String cityFolder() {
        return cityName.replace(' ', '_');
    }
}
