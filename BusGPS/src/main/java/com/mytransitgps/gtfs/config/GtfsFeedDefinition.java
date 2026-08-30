package com.mytransitgps.gtfs.config;

/**
 * 单个 GTFS Feed 的配置定义，包括城市、Realtime URL 和 Static URL。
 */
public record GtfsFeedDefinition(
        String feedId,
        String cityCode,
        String cityFolder,
        String cityName,
        String operator,
        String serviceType,
        String realtimeUrl,
        String staticUrl,
        String filePrefix,
        boolean enabled) {
}
