package com.mytransitgps.gtfs.model;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.mytransitgps.gtfs.client.GtfsHttpResult;
import com.mytransitgps.gtfs.config.GtfsFeedDefinition;

/**
 * 单个 Feed 在多城市运行中的请求、文件和车辆统计结果。
 */
public record MultiCityFeedRunResult(
        GtfsFeedDefinition feed,
        String batchId,
        Instant malaysiaStartedAt,
        Instant malaysiaFinishedAt,
        GtfsHttpResult staticHttpResult,
        GtfsHttpResult realtimeHttpResult,
        StaticFeedData staticFeedData,
        FieldPresenceAudit fieldPresenceAudit,
        int entityCount,
        int vehicleCount,
        int uniqueRouteCount,
        int routeDirectMatchCount,
        int routeTripFallbackMatchCount,
        int routeResolvedCount,
        int routeUnresolvedCount,
        int tripMatchCount,
        int tripUnmatchedCount,
        int directionMatchCount,
        int directionMismatchCount,
        int directionNotComparableCount,
        Path rawPbPath,
        Path parsedJsonPath,
        Path enrichedJsonPath,
        Path realtimeMetadataPath,
        Path staticMetadataPath,
        List<Map<String, Object>> enrichedVehicles,
        Map<String, Long> activeVehiclesByRoute) {

    public boolean realtimeSuccess() {
        return realtimeHttpResult != null && realtimeHttpResult.statusCode() == 200;
    }

    public boolean staticSuccess() {
        return staticHttpResult != null && staticHttpResult.statusCode() == 200;
    }
}
