package com.mytransitgps.gtfs.service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.transit.realtime.GtfsRealtime;
import com.mytransitgps.gtfs.client.GtfsApiClient;
import com.mytransitgps.gtfs.client.GtfsHttpResult;
import com.mytransitgps.gtfs.config.GtfsFeedDefinition;
import com.mytransitgps.gtfs.config.GtfsFeedRegistry;
import com.mytransitgps.gtfs.model.FieldPresenceAudit;
import com.mytransitgps.gtfs.model.MultiCityDemoResult;
import com.mytransitgps.gtfs.model.MultiCityFeedRunResult;
import com.mytransitgps.gtfs.model.StaticFeedData;
import com.mytransitgps.gtfs.parser.GtfsRealtimeParser;
import com.mytransitgps.gtfs.parser.GtfsStaticParser;
import com.mytransitgps.gtfs.service.RealtimeStaticEnrichmentService.EnrichmentResult;
import com.mytransitgps.gtfs.service.StaticArchiveService.StaticArchiveResult;
import com.mytransitgps.gtfs.util.GtfsTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 多城市 GTFS 演示运行器，串联下载、解析、补全、质检和报告输出流程。
 */
public class MultiCityGtfsDemoRunner {

    private static final Logger log = LoggerFactory.getLogger(MultiCityGtfsDemoRunner.class);
    private final Path workspaceRoot;
    private final GtfsFeedRegistry feedRegistry;
    private final GtfsApiClient apiClient;
    private final GtfsRealtimeParser realtimeParser;
    private final GtfsStaticParser staticParser;
    private final RawArchiveService rawArchiveService;
    private final JsonOutputService jsonOutputService;
    private final StaticArchiveService staticArchiveService;
    private final RealtimeStaticEnrichmentService enrichmentService;
    private final FieldPresenceAuditService fieldPresenceAuditService;
    private final FieldPresenceMatrixService fieldPresenceMatrixService;

    public MultiCityGtfsDemoRunner(Path workspaceRoot) {
        this.workspaceRoot = workspaceRoot;
        this.feedRegistry = new GtfsFeedRegistry();
        this.apiClient = new GtfsApiClient();
        this.realtimeParser = new GtfsRealtimeParser();
        this.staticParser = new GtfsStaticParser();
        this.rawArchiveService = new RawArchiveService();
        this.jsonOutputService = new JsonOutputService();
        this.staticArchiveService = new StaticArchiveService(jsonOutputService);
        this.enrichmentService = new RealtimeStaticEnrichmentService(jsonOutputService);
        this.fieldPresenceAuditService = new FieldPresenceAuditService();
        this.fieldPresenceMatrixService = new FieldPresenceMatrixService(jsonOutputService);
    }

    public MultiCityDemoResult runDemo() {
        Instant startedAt = Instant.now();
        LocalDate batchDate = GtfsTime.malaysiaToday();
        String batchId = GtfsTime.formatBatchDate(batchDate) + "_" + GtfsTime.formatFileTimestamp(startedAt).substring(9) + "_"
                + UUID.randomUUID().toString().substring(0, 4);
        List<GtfsFeedDefinition> enabledFeeds = feedRegistry.getEnabledFeeds();
        log.info("多城市GTFS演示任务启动，batchId={}，batchDate={}，feedCount={}，workspaceRoot={}",
                batchId, batchDate, enabledFeeds.size(), workspaceRoot);

        List<MultiCityFeedRunResult> results = new ArrayList<>();
        for (GtfsFeedDefinition feed : enabledFeeds) {
            results.add(runSingleFeed(batchId, batchDate, feed));
        }
        Instant finishedAt = Instant.now();
        FieldPresenceMatrixService.MatrixPaths matrixPaths = writeFieldPresenceMatrix(batchDate, finishedAt, results);
        long successfulFeeds = results.stream().filter(result -> result.realtimeHttpResult() != null && result.realtimeHttpResult().isHttpOk()).count();
        log.info("多城市GTFS演示任务完成，batchId={}，feedCount={}，successfulRealtimeFeeds={}，failedRealtimeFeeds={}，durationMs={}，matrixJson={}，matrixCsv={}",
                batchId, results.size(), successfulFeeds, results.size() - successfulFeeds,
                java.time.Duration.between(startedAt, finishedAt).toMillis(), matrixPaths.jsonPath(), matrixPaths.csvPath());
        return new MultiCityDemoResult(batchId, batchDate, startedAt, finishedAt, List.copyOf(results), matrixPaths.jsonPath(), matrixPaths.csvPath());
    }

    private MultiCityFeedRunResult runSingleFeed(String batchId, LocalDate batchDate, GtfsFeedDefinition feed) {
        Instant startedAt = Instant.now();
        log.info("开始处理多城市GTFS Feed，batchId={}，城市={}，feedId={}", batchId, feed.cityName(), feed.feedId());
        String batchDateText = GtfsTime.formatBatchDate(batchDate);

        log.info("开始调用GTFS Static接口下载数据，batchId={}，城市={}，feedId={}",
                batchId, feed.cityName(), feed.feedId());
        GtfsHttpResult staticHttpResult = apiClient.fetch("STATIC", feed.staticUrl());
        StaticFeedData staticFeedData = emptyStaticFeedData();
        Path staticMetadataPath = metadataPath(batchDateText, feed, "static", staticHttpResult.responseReceivedAt());
        String staticErrorClass = staticHttpResult.errorClass();
        String staticErrorMessage = staticHttpResult.errorMessage();
        StaticArchiveResult staticArchiveResult = null;

        if (staticHttpResult.isHttpOk()) {
            try {
                staticFeedData = staticParser.parse(staticHttpResult.responseBody());
                staticArchiveResult = staticArchiveService.archive(
                        workspaceRoot,
                        feed.feedId(),
                        staticHttpResult.responseSha256(),
                        staticHttpResult.responseBody());
            } catch (Exception ex) {
                staticErrorClass = ex.getClass().getSimpleName();
                staticErrorMessage = ex.getMessage();
                log.error("Static GTFS处理失败，batchId={}，城市={}，feedId={}，错误信息={}",
                        batchId, feed.cityName(), feed.feedId(), ex.getMessage(), ex);
            }
        }

        try {
            jsonOutputService.writeJsonObject(staticMetadataPath, buildStaticMetadata(
                    batchId,
                    feed,
                    staticHttpResult,
                    staticFeedData,
                    staticArchiveResult,
                    staticErrorClass,
                    staticErrorMessage));
        } catch (IOException ex) {
            log.error("Static元数据写入失败，batchId={}，feedId={}，path={}，错误信息={}",
                    batchId, feed.feedId(), staticMetadataPath, ex.getMessage(), ex);
            throw new IllegalStateException("Failed to write static metadata for feed " + feed.feedId(), ex);
        }

        log.info("开始调用GTFS Realtime接口下载数据，batchId={}，城市={}，feedId={}",
                batchId, feed.cityName(), feed.feedId());
        GtfsHttpResult realtimeHttpResult = apiClient.fetch("REALTIME", feed.realtimeUrl());
        Path realtimeMetadataPath = metadataPath(batchDateText, feed, "realtime", realtimeHttpResult.responseReceivedAt());
        Path rawPbPath = null;
        Path parsedJsonPath = null;
        Path enrichedJsonPath = null;
        FieldPresenceAudit fieldPresenceAudit = new FieldPresenceAudit.Builder(feed.feedId(), 0).build();
        List<Map<String, Object>> enrichedVehicles = List.of();
        Map<String, Long> activeVehiclesByRoute = Map.of();
        int entityCount = 0;
        int vehicleCount = 0;
        int uniqueRouteCount = 0;
        int routeDirectMatchCount = 0;
        int routeTripFallbackMatchCount = 0;
        int routeResolvedCount = 0;
        int routeUnresolvedCount = 0;
        int tripMatchCount = 0;
        int tripUnmatchedCount = 0;
        int directionMatchCount = 0;
        int directionMismatchCount = 0;
        int directionNotComparableCount = 0;
        String realtimeErrorClass = realtimeHttpResult.errorClass();
        String realtimeErrorMessage = realtimeHttpResult.errorMessage();
        String parseStatus = "SKIPPED";

        if (realtimeHttpResult.isHttpOk()) {
            try {
                rawPbPath = rawArchiveService.save(
                        realtimePbPath(batchDateText, feed, realtimeHttpResult.responseReceivedAt()),
                        realtimeHttpResult.responseBody());
                GtfsRealtime.FeedMessage feedMessage = realtimeParser.parse(realtimeHttpResult.responseBody());
                parseStatus = "PARSED";
                entityCount = feedMessage.getEntityCount();
                parsedJsonPath = jsonOutputService.writeJsonString(
                        parsedJsonPath(batchDateText, feed, realtimeHttpResult.responseReceivedAt()),
                        realtimeParser.toJson(feedMessage));
                EnrichmentResult enrichmentResult = enrichmentService.enrich(
                        batchId,
                        feed,
                        feedMessage,
                        staticFeedData,
                        realtimeHttpResult);
                enrichedJsonPath = jsonOutputService.writeJsonObject(
                        enrichedJsonPath(batchDateText, feed, realtimeHttpResult.responseReceivedAt()),
                        enrichmentResult.root());
                enrichedVehicles = enrichmentResult.vehicles();
                activeVehiclesByRoute = enrichmentResult.activeVehiclesByRoute();
                vehicleCount = enrichmentResult.vehicleCount();
                uniqueRouteCount = enrichmentResult.uniqueRouteCount();
                routeDirectMatchCount = enrichmentResult.routeDirectMatchCount();
                routeTripFallbackMatchCount = enrichmentResult.routeTripFallbackMatchCount();
                routeResolvedCount = enrichmentResult.routeResolvedCount();
                routeUnresolvedCount = enrichmentResult.routeUnresolvedCount();
                tripMatchCount = enrichmentResult.tripMatchCount();
                tripUnmatchedCount = enrichmentResult.tripUnmatchedCount();
                directionMatchCount = enrichmentResult.directionMatchCount();
                directionMismatchCount = enrichmentResult.directionMismatchCount();
                directionNotComparableCount = enrichmentResult.directionNotComparableCount();
                fieldPresenceAudit = fieldPresenceAuditService.audit(feed.feedId(), feedMessage);
            } catch (Exception ex) {
                realtimeErrorClass = ex.getClass().getSimpleName();
                realtimeErrorMessage = ex.getMessage();
                parseStatus = "FAILED";
                log.error("Realtime处理失败，batchId={}，城市={}，feedId={}，错误信息={}",
                        batchId, feed.cityName(), feed.feedId(), ex.getMessage(), ex);
            }
        }

        try {
            jsonOutputService.writeJsonObject(realtimeMetadataPath, buildRealtimeMetadata(
                    batchId,
                    feed,
                    realtimeHttpResult,
                    parseStatus,
                    entityCount,
                    vehicleCount,
                    rawPbPath,
                    parsedJsonPath,
                    enrichedJsonPath,
                    realtimeErrorClass,
                    realtimeErrorMessage));
        } catch (IOException ex) {
            log.error("Realtime元数据写入失败，batchId={}，feedId={}，path={}，错误信息={}",
                    batchId, feed.feedId(), realtimeMetadataPath, ex.getMessage(), ex);
            throw new IllegalStateException("Failed to write realtime metadata for feed " + feed.feedId(), ex);
        }
        if (realtimeHttpResult.isHttpOk() && "PARSED".equals(parseStatus)) {
            log.info("多城市GTFS Feed处理完成，batchId={}，城市={}，feedId={}，httpStatus={}，entities={}，vehicles={}，routeResolved={}，tripMatched={}，durationMs={}",
                    batchId, feed.cityName(), feed.feedId(), realtimeHttpResult.statusCode(), entityCount, vehicleCount,
                    routeResolvedCount, tripMatchCount, java.time.Duration.between(startedAt, Instant.now()).toMillis());
        } else {
            log.warn("多城市GTFS Feed处理未完全成功，batchId={}，城市={}，feedId={}，httpStatus={}，parseStatus={}，errorClass={}，errorMessage={}",
                    batchId, feed.cityName(), feed.feedId(), realtimeHttpResult.statusCode(), parseStatus, realtimeErrorClass, realtimeErrorMessage);
        }

        return new MultiCityFeedRunResult(
                feed,
                batchId,
                startedAt,
                Instant.now(),
                staticHttpResult,
                realtimeHttpResult,
                staticFeedData,
                fieldPresenceAudit,
                entityCount,
                vehicleCount,
                uniqueRouteCount,
                routeDirectMatchCount,
                routeTripFallbackMatchCount,
                routeResolvedCount,
                routeUnresolvedCount,
                tripMatchCount,
                tripUnmatchedCount,
                directionMatchCount,
                directionMismatchCount,
                directionNotComparableCount,
                rawPbPath,
                parsedJsonPath,
                enrichedJsonPath,
                realtimeMetadataPath,
                staticMetadataPath,
                enrichedVehicles,
                activeVehiclesByRoute);
    }

    private Map<String, Object> buildStaticMetadata(
            String batchId,
            GtfsFeedDefinition feed,
            GtfsHttpResult httpResult,
            StaticFeedData staticFeedData,
            StaticArchiveResult archiveResult,
            String errorClass,
            String errorMessage) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("batch_id", batchId);
        metadata.put("city", feed.cityName());
        metadata.put("city_code", feed.cityCode());
        metadata.put("feed_id", feed.feedId());
        metadata.put("operator", feed.operator());
        metadata.put("service_type", feed.serviceType());
        metadata.put("request_type", "STATIC");
        metadata.put("requested_url", httpResult.requestedUri().toString());
        metadata.put("final_url", httpResult.finalUri().toString());
        metadata.put("request_started_at", httpResult.requestStartedAt().toString());
        metadata.put("response_received_at", httpResult.responseReceivedAt().toString());
        metadata.put("ingest_timestamp_utc", httpResult.ingestTimestampUtc().toString());
        metadata.put("http_status", httpResult.statusCode());
        metadata.put("content_type", httpResult.contentType());
        metadata.put("latency_ms", httpResult.latencyMs());
        metadata.put("zip_bytes", httpResult.responseBytes());
        metadata.put("zip_sha256", httpResult.responseSha256());
        metadata.put("redirect_count", httpResult.redirectCount());
        metadata.put("static_object_path", archiveResult == null ? null : workspaceRoot.relativize(archiveResult.objectPath()).toString().replace('\\', '/'));
        metadata.put("routes_count", staticFeedData.routes().size());
        metadata.put("trips_count", staticFeedData.trips().size());
        metadata.put("stops_count", staticFeedData.stopsCount());
        metadata.put("stop_times_count", staticFeedData.stopTimesCount());
        metadata.put("shapes_present", staticFeedData.shapesPresent());
        metadata.put("calendar_present", staticFeedData.calendarPresent());
        metadata.put("calendar_dates_present", staticFeedData.calendarDatesPresent());
        metadata.put("frequencies_present", staticFeedData.frequenciesPresent());
        metadata.put("changed_from_previous", archiveResult != null && archiveResult.changedFromPrevious());
        metadata.put("error_class", errorClass);
        metadata.put("error_message", errorMessage);
        metadata.put("result", httpResult.isHttpOk() && errorClass == null ? "SUCCESS" : "FAILED");
        return metadata;
    }

    private Map<String, Object> buildRealtimeMetadata(
            String batchId,
            GtfsFeedDefinition feed,
            GtfsHttpResult httpResult,
            String parseStatus,
            int entityCount,
            int vehicleCount,
            Path rawPbPath,
            Path parsedJsonPath,
            Path enrichedJsonPath,
            String errorClass,
            String errorMessage) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("batch_id", batchId);
        metadata.put("city_code", feed.cityCode());
        metadata.put("city_name", feed.cityName());
        metadata.put("feed_id", feed.feedId());
        metadata.put("operator", feed.operator());
        metadata.put("service_type", feed.serviceType());
        metadata.put("request_type", "REALTIME");
        metadata.put("requested_url", httpResult.requestedUri().toString());
        metadata.put("final_url", httpResult.finalUri().toString());
        metadata.put("request_started_at", httpResult.requestStartedAt().toString());
        metadata.put("response_received_at", httpResult.responseReceivedAt().toString());
        metadata.put("ingest_timestamp_utc", httpResult.ingestTimestampUtc().toString());
        metadata.put("http_status", httpResult.statusCode());
        metadata.put("content_type", httpResult.contentType());
        metadata.put("latency_ms", httpResult.latencyMs());
        metadata.put("response_bytes", httpResult.responseBytes());
        metadata.put("response_sha256", httpResult.responseSha256());
        metadata.put("redirect_count", httpResult.redirectCount());
        metadata.put("parse_status", parseStatus);
        metadata.put("entity_count", entityCount);
        metadata.put("vehicle_count", vehicleCount);
        metadata.put("raw_pb_path", toRelativeString(rawPbPath));
        metadata.put("parsed_json_path", toRelativeString(parsedJsonPath));
        metadata.put("enriched_json_path", toRelativeString(enrichedJsonPath));
        metadata.put("error_class", errorClass);
        metadata.put("error_message", errorMessage);
        metadata.put("result", httpResult.isHttpOk() && "PARSED".equals(parseStatus) ? "SUCCESS" : "FAILED");
        return metadata;
    }

    private FieldPresenceMatrixService.MatrixPaths writeFieldPresenceMatrix(
            LocalDate batchDate,
            Instant finishedAt,
            List<MultiCityFeedRunResult> results) {
        try {
            Map<String, String> displayNames = new LinkedHashMap<>();
            displayNames.put("mybas-johor", "JB");
            displayNames.put("mybas-kuching", "Kuching");
            displayNames.put("rapid-bus-kl", "KL Bus");
            displayNames.put("rapid-bus-mrtfeeder", "MRT Feeder");
            displayNames.put("mybas-melaka", "Melaka");
            List<FieldPresenceAudit> audits = results.stream()
                    .sorted(Comparator.comparing(result -> result.feed().feedId()))
                    .map(MultiCityFeedRunResult::fieldPresenceAudit)
                    .toList();
            return fieldPresenceMatrixService.writeMatrix(
                    BusGpsStorageLayout.dailyJsonRoot(workspaceRoot, GtfsTime.formatBatchDate(batchDate)),
                    GtfsTime.formatFileTimestamp(finishedAt),
                    audits,
                    displayNames);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write field presence matrix.", ex);
        }
    }

    private Path metadataPath(String batchDateText, GtfsFeedDefinition feed, String type, Instant timestamp) {
        String prefix = feed.filePrefix() + "_gtfs_" + type + "_metadata_" + GtfsTime.formatFileTimestamp(timestamp) + ".json";
        return BusGpsStorageLayout.dailyJsonRoot(workspaceRoot, batchDateText).resolve(feed.cityFolder()).resolve(prefix);
    }

    private Path parsedJsonPath(String batchDateText, GtfsFeedDefinition feed, Instant timestamp) {
        String fileName = feed.filePrefix() + "_gtfs_realtime_parsed_full_" + GtfsTime.formatFileTimestamp(timestamp) + ".json";
        return BusGpsStorageLayout.dailyJsonRoot(workspaceRoot, batchDateText).resolve(feed.cityFolder()).resolve(fileName);
    }

    private Path enrichedJsonPath(String batchDateText, GtfsFeedDefinition feed, Instant timestamp) {
        String fileName = feed.filePrefix() + "_gtfs_realtime_enriched_full_" + GtfsTime.formatFileTimestamp(timestamp) + ".json";
        return BusGpsStorageLayout.dailyJsonRoot(workspaceRoot, batchDateText).resolve(feed.cityFolder()).resolve(fileName);
    }

    private Path realtimePbPath(String batchDateText, GtfsFeedDefinition feed, Instant timestamp) {
        String fileName = feed.filePrefix() + "_gtfs_realtime_" + GtfsTime.formatFileTimestamp(timestamp) + ".pb";
        return workspaceRoot.resolve("raw_data").resolve(batchDateText).resolve(feed.cityFolder()).resolve(fileName);
    }

    private String toRelativeString(Path path) {
        if (path == null) {
            return null;
        }
        return workspaceRoot.relativize(path).toString().replace('\\', '/');
    }

    private StaticFeedData emptyStaticFeedData() {
        return new StaticFeedData(Map.of(), Map.of(), 0, 0, false, false, false, false, false, false, false, false);
    }
}
