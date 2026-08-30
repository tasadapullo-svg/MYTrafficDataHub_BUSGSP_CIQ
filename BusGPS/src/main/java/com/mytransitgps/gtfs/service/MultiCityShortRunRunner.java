package com.mytransitgps.gtfs.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.transit.realtime.GtfsRealtime;
import com.mytransitgps.gtfs.client.GtfsApiClient;
import com.mytransitgps.gtfs.client.GtfsHttpResult;
import com.mytransitgps.gtfs.config.GtfsFeedDefinition;
import com.mytransitgps.gtfs.config.GtfsFeedRegistry;
import com.mytransitgps.gtfs.model.FieldPresenceAudit;
import com.mytransitgps.gtfs.model.FeedSpatialBounds;
import com.mytransitgps.gtfs.model.GpsJumpQcResult;
import com.mytransitgps.gtfs.model.PositionQcResult;
import com.mytransitgps.gtfs.model.ScheduledTripTimeValidationResult;
import com.mytransitgps.gtfs.model.ShapePoint;
import com.mytransitgps.gtfs.model.ShortRunResult;
import com.mytransitgps.gtfs.model.StaticFeedData;
import com.mytransitgps.gtfs.model.StaticShapeQcResult;
import com.mytransitgps.gtfs.parser.GtfsRealtimeParser;
import com.mytransitgps.gtfs.parser.GtfsStaticParser;
import com.mytransitgps.gtfs.service.FieldPresenceMatrixService.MatrixPaths;
import com.mytransitgps.gtfs.service.RealtimeStaticEnrichmentService.EnrichmentResult;
import com.mytransitgps.gtfs.service.StaticArchiveService.StaticArchiveResult;
import com.mytransitgps.gtfs.util.GtfsTime;
import com.mytransitgps.gtfs.util.HashUtils;
import com.mytransitgps.gtfs.util.StatsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 多城市短时采集运行器，按错峰调度执行多个 Feed 并生成完整磁盘证据。
 */
public class MultiCityShortRunRunner {

    private static final Logger log = LoggerFactory.getLogger(MultiCityShortRunRunner.class);
    private static final int DEFAULT_PLANNED_CYCLES = 5;
    private static final int DEFAULT_FEED_INTERVAL_SECONDS = 120;
    private static final int DEFAULT_FEED_STAGGER_SECONDS = 24;

    private final Path workspaceRoot;
    private final int plannedCycles;
    private final int feedIntervalSeconds;
    private final int feedStaggerSeconds;
    private final RunProfile runProfile;
    private final GtfsFeedRegistry feedRegistry;
    private final JsonOutputService jsonOutputService;
    private final GtfsRealtimeParser realtimeParser;
    private final GtfsStaticParser staticParser;
    private final StaticArchiveService staticArchiveService;
    private final RealtimeStaticEnrichmentService enrichmentService;
    private final FieldPresenceAuditService fieldPresenceAuditService;
    private final FieldPresenceMatrixService fieldPresenceMatrixService;
    private final RawArchiveService rawArchiveService;
    private final PositionQcService positionQcService;
    private final FeedBoundsService feedBoundsService;
    private final GpsJumpQcService gpsJumpQcService;
    private final GapFreshnessSummaryService gapFreshnessSummaryService;
    private final StaticShapeQcService staticShapeQcService;
    private final ScheduledTripTimeValidationService scheduledTripTimeValidationService;
    private final ObservationEligibilityPolicy observationEligibilityPolicy;

    public MultiCityShortRunRunner(Path workspaceRoot) {
        this(workspaceRoot, DEFAULT_PLANNED_CYCLES, DEFAULT_FEED_INTERVAL_SECONDS, DEFAULT_FEED_STAGGER_SECONDS, RunProfile.shortRun());
    }

    public MultiCityShortRunRunner(Path workspaceRoot, int plannedCycles, int feedIntervalSeconds, int feedStaggerSeconds) {
        this(workspaceRoot, plannedCycles, feedIntervalSeconds, feedStaggerSeconds, RunProfile.shortRun());
    }

    private MultiCityShortRunRunner(Path workspaceRoot, int plannedCycles, int feedIntervalSeconds, int feedStaggerSeconds, RunProfile runProfile) {
        this.workspaceRoot = workspaceRoot;
        this.plannedCycles = plannedCycles;
        this.feedIntervalSeconds = feedIntervalSeconds;
        this.feedStaggerSeconds = feedStaggerSeconds;
        this.runProfile = runProfile;
        this.feedRegistry = new GtfsFeedRegistry();
        this.jsonOutputService = new JsonOutputService();
        this.realtimeParser = new GtfsRealtimeParser();
        this.staticParser = new GtfsStaticParser();
        this.staticArchiveService = new StaticArchiveService(jsonOutputService);
        this.enrichmentService = new RealtimeStaticEnrichmentService(jsonOutputService);
        this.fieldPresenceAuditService = new FieldPresenceAuditService();
        this.fieldPresenceMatrixService = new FieldPresenceMatrixService(jsonOutputService);
        this.rawArchiveService = new RawArchiveService();
        this.positionQcService = new PositionQcService();
        this.feedBoundsService = new FeedBoundsService(10.0d);
        this.gpsJumpQcService = new GpsJumpQcService(100.0d, 130.0d, 600L);
        this.gapFreshnessSummaryService = new GapFreshnessSummaryService();
        this.staticShapeQcService = new StaticShapeQcService(10.0d);
        this.scheduledTripTimeValidationService = new ScheduledTripTimeValidationService();
        this.observationEligibilityPolicy = new ObservationEligibilityPolicy();
    }

    public static MultiCityShortRunRunner oneHourRunner(Path workspaceRoot) {
        return new MultiCityShortRunRunner(workspaceRoot, 30, 120, 24, RunProfile.oneHour());
    }

    public ShortRunResult runShortRun() {
        // 在任何网络请求前验证周期与错峰参数，并用锁文件防止重复运行。
        validateSchedule();
        List<GtfsFeedDefinition> feeds = feedRegistry.getEnabledFeeds();
        if (feeds.size() != 5) {
            throw new IllegalStateException("Run requires exactly 5 enabled feeds.");
        }

        LocalDate batchDate = GtfsTime.malaysiaToday();
        String batchDateText = GtfsTime.formatBatchDate(batchDate);
        String runId = runProfile.runIdPrefix + "_" + GtfsTime.formatFileTimestamp(Instant.now()) + "_" + UUID.randomUUID().toString().substring(0, 4);
        Path lockPath = workspaceRoot.resolve("runtime").resolve(runProfile.lockFileName);
        log.info("多城市短时采集运行准备启动，runId={}，profile={}，feedCount={}，plannedCycles={}，intervalSeconds={}，staggerSeconds={}，workspaceRoot={}",
                runId, runProfile.runIdPrefix, feeds.size(), plannedCycles, feedIntervalSeconds, feedStaggerSeconds, workspaceRoot);
        acquireLock(lockPath, runId);

        try {
            // Realtime 处理依赖同一轮准备好的 Static 索引，保证路线补全版本一致。
            Map<String, PreparedStaticFeed> preparedStatic = prepareStaticFeeds(batchDateText, runId, feeds);
            Instant runStartedAt = Instant.now();
            log.info("多城市短时采集正式开始，runId={}，startedAt={}，plannedRequests={}",
                    runId, runStartedAt, feeds.size() * plannedCycles);
            GtfsApiClient realtimeClient = new GtfsApiClient(java.time.Clock.systemUTC(), Duration.ZERO);

            Map<String, FeedRunState> feedStates = new LinkedHashMap<>();
            for (GtfsFeedDefinition feed : feeds) {
                feedStates.put(feed.feedId(), new FeedRunState(feed, preparedStatic.get(feed.feedId())));
            }

            List<Map<String, Object>> snapshotRows = new ArrayList<>();
            int successfulRequests = 0;
            int failedRequests = 0;
            int http429Count = 0;
            int http5xxCount = 0;
            int timeoutCount = 0;

            int snapshotSequence = 0;
            for (int cycle = 1; cycle <= plannedCycles; cycle++) {
                for (int feedIndex = 0; feedIndex < feeds.size(); feedIndex++) {
                    GtfsFeedDefinition feed = feeds.get(feedIndex);
                    FeedRunState feedState = feedStates.get(feed.feedId());
                    // 使用绝对计划时间而非“请求结束后再等待”，避免网络耗时导致周期持续漂移。
                    Instant scheduledAt = runStartedAt.plusSeconds((long) (cycle - 1) * feedIntervalSeconds + (long) feedIndex * feedStaggerSeconds);
                    sleepUntil(scheduledAt);
                    snapshotSequence++;
                    log.info("开始调用GTFS Realtime接口下载数据，runId={}，城市={}，feedId={}，cycle={}，snapshotSequence={}，scheduledAt={}",
                            runId, feed.cityName(), feed.feedId(), cycle, snapshotSequence, scheduledAt);

                    GtfsHttpResult httpResult = realtimeClient.fetch("REALTIME", feed.realtimeUrl());
                    SnapshotProcessResult processed = processRealtimeSnapshot(runId, batchDateText, cycle, snapshotSequence, scheduledAt, httpResult, feedState);
                    snapshotRows.add(processed.snapshotSummary());
                    log.info("Realtime轮次处理完成，runId={}，城市={}，feedId={}，cycle={}，httpStatus={}，vehicleCount={}，duplicateSnapshot={}，schedulerDriftMs={}",
                            runId, feed.cityName(), feed.feedId(), cycle, httpResult.statusCode(),
                            processed.snapshotSummary().get("vehicle_count"), processed.snapshotSummary().get("duplicate_snapshot"),
                            processed.snapshotSummary().get("scheduler_drift_ms"));

                    if (httpResult.isHttpOk()) {
                        successfulRequests++;
                    } else {
                        failedRequests++;
                    }
                    if (httpResult.statusCode() == 429) {
                        http429Count++;
                    }
                    if (httpResult.statusCode() >= 500 && httpResult.statusCode() <= 599) {
                        http5xxCount++;
                    }
                    if ("HttpTimeoutException".equals(httpResult.errorClass())) {
                        timeoutCount++;
                    }

                    feedState.actualRequestCount++;
                    feedState.schedulerDriftMs.add(Duration.between(scheduledAt, httpResult.requestStartedAt()).toMillis());
                    feedState.requestStartedAtList.add(httpResult.requestStartedAt());
                }
            }

            Instant runFinishedAt = Instant.now();
            Path runReportRoot = workspaceRoot.resolve("run_reports").resolve(batchDateText).resolve(runId);
            Files.createDirectories(runReportRoot);

            MatrixPaths matrixPaths = writeFieldPresence(runId, batchDateText, runReportRoot, feedStates);
            Path positionQcSummaryPath = writePositionQcSummary(runReportRoot, feedStates);
            Path gpsJumpEventsPath = writeGpsJumpEventsCsv(runReportRoot, feedStates);
            Path gapFreshnessSummaryPath = writeGapFreshnessSummary(runReportRoot, feedStates);
            Path feedSpatialBoundsPath = writeFeedSpatialBounds(runReportRoot, feedStates);
            Path staticShapeQcPath = writeStaticShapeQc(runReportRoot, feedStates);
            Path routeShapeLengthJsonPath = writeRouteShapeLengthJson(runReportRoot, feedStates);
            Path routeShapeLengthCsvPath = writeRouteShapeLengthCsv(runReportRoot, feedStates);
            Path scheduledTripTimeQcPath = writeScheduledTripTimeQcJson(runReportRoot, feedStates);
            Path scheduledTripTimeCsvPath = writeScheduledTripTimeCsv(runReportRoot, feedStates);
            Path scheduledRouteTimeSummaryCsvPath = writeScheduledRouteTimeSummaryCsv(runReportRoot, feedStates);

            List<Map<String, Object>> feedSummaries = buildFeedSummaries(feedStates);
            Path manifestPath = writeManifest(
                    runReportRoot,
                    batchDateText,
                    runId,
                    runStartedAt,
                    runFinishedAt,
                    successfulRequests,
                    failedRequests,
                    http429Count,
                    http5xxCount,
                    timeoutCount,
                    feedSummaries,
                    positionQcSummaryPath,
                    gpsJumpEventsPath,
                    gapFreshnessSummaryPath,
                    feedSpatialBoundsPath,
                    staticShapeQcPath,
                    routeShapeLengthJsonPath,
                    scheduledTripTimeQcPath);
            Path feedSummaryCsvPath = writeFeedSummaryCsv(runReportRoot, runId, feedSummaries);
            Path snapshotComparisonCsvPath = writeSnapshotSummaryCsv(runReportRoot, runId, snapshotRows);

            jsonOutputService.writeJsonObject(BusGpsStorageLayout.dailyJsonRoot(workspaceRoot, batchDateText).resolve(runProfile.topLevelManifestName(runId)),
                    jsonOutputService.objectMapper().readTree(manifestPath.toFile()));
            log.info("多城市短时采集运行完成，runId={}，actualRequests={}，successful={}，failed={}，http429={}，http5xx={}，timeouts={}，durationMs={}，reportRoot={}",
                    runId, snapshotRows.size(), successfulRequests, failedRequests, http429Count, http5xxCount, timeoutCount,
                    Duration.between(runStartedAt, runFinishedAt).toMillis(), runReportRoot);

            return new ShortRunResult(
                    runId,
                    batchDate,
                    runStartedAt,
                    runFinishedAt,
                    plannedCycles,
                    plannedCycles,
                    feeds.size() * plannedCycles,
                    snapshotRows.size(),
                    successfulRequests,
                    failedRequests,
                    http429Count,
                    http5xxCount,
                    timeoutCount,
                    feedSummaries,
                    snapshotRows,
                    manifestPath,
                    feedSummaryCsvPath,
                    snapshotComparisonCsvPath,
                    matrixPaths.jsonPath(),
                    matrixPaths.csvPath(),
                    runReportRoot);
        } catch (IOException ex) {
            log.error("多城市短时采集运行失败，runId={}，错误信息={}", runId, ex.getMessage(), ex);
            throw new IllegalStateException("Run failed: " + ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            log.error("多城市短时采集运行失败，runId={}，异常类型={}，错误信息={}",
                    runId, ex.getClass().getSimpleName(), ex.getMessage(), ex);
            throw ex;
        } finally {
            releaseLock(lockPath);
        }
    }

    private Map<String, PreparedStaticFeed> prepareStaticFeeds(String batchDateText, String runId, List<GtfsFeedDefinition> feeds) throws IOException {
        // 每个 Feed 的 Static 下载、解析、归档与质检互相隔离，失败信息也会写入元数据。
        GtfsApiClient staticClient = new GtfsApiClient();
        Map<String, PreparedStaticFeed> prepared = new LinkedHashMap<>();
        for (GtfsFeedDefinition feed : feeds) {
            log.info("开始调用GTFS Static接口下载数据，runId={}，城市={}，feedId={}",
                    runId, feed.cityName(), feed.feedId());
            GtfsHttpResult httpResult = staticClient.fetch("STATIC", feed.staticUrl());
            StaticFeedData staticFeedData = new StaticFeedData(Map.of(), Map.of(), 0, 0, false, false, false, false, false, false, false, false);
            StaticArchiveResult archiveResult = null;
            String errorClass = httpResult.errorClass();
            String errorMessage = httpResult.errorMessage();
            if (httpResult.isHttpOk()) {
                try {
                    staticFeedData = staticParser.parse(httpResult.responseBody());
                    archiveResult = staticArchiveService.archive(workspaceRoot, feed.feedId(), httpResult.responseSha256(), httpResult.responseBody());
                } catch (Exception ex) {
                    errorClass = ex.getClass().getSimpleName();
                    errorMessage = ex.getMessage();
                    log.error("Static GTFS准备失败，runId={}，城市={}，feedId={}，错误信息={}",
                            runId, feed.cityName(), feed.feedId(), ex.getMessage(), ex);
                }
            }

            FeedSpatialBounds bounds = feedBoundsService.compute(feed.feedId(), httpResult.responseSha256(), staticFeedData);
            StaticShapeQcResult staticShapeQcResult = staticShapeQcService.analyze(feed.feedId(), staticFeedData);
            ScheduledTripTimeValidationResult scheduledTimeResult = scheduledTripTimeValidationService.analyze(feed.feedId(), staticFeedData);

            Path metadataPath = BusGpsStorageLayout.dailyJsonRoot(workspaceRoot, batchDateText).resolve(feed.cityFolder())
                    .resolve(feed.filePrefix() + "_gtfs_static_metadata_" + GtfsTime.formatFileTimestamp(httpResult.responseReceivedAt()) + ".json");
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("run_id", runId);
            metadata.put("feed_id", feed.feedId());
            metadata.put("city", feed.cityName());
            metadata.put("request_type", "STATIC");
            metadata.put("requested_url", httpResult.requestedUri().toString());
            metadata.put("final_url", httpResult.finalUri().toString());
            metadata.put("request_started_at", httpResult.requestStartedAt().toString());
            metadata.put("response_received_at", httpResult.responseReceivedAt().toString());
            metadata.put("http_status", httpResult.statusCode());
            metadata.put("content_type", httpResult.contentType());
            metadata.put("latency_ms", httpResult.latencyMs());
            metadata.put("zip_bytes", httpResult.responseBytes());
            metadata.put("zip_sha256", httpResult.responseSha256());
            metadata.put("static_object_path", archiveResult == null ? null : relativize(archiveResult.objectPath()));
            metadata.put("static_version_source", "HTTP_FETCH");
            metadata.put("routes_count", staticFeedData.routes().size());
            metadata.put("trips_count", staticFeedData.trips().size());
            metadata.put("stops_count", staticFeedData.stopsCount());
            metadata.put("stop_times_count", staticFeedData.stopTimesCount());
            metadata.put("shapes_count", staticFeedData.shapesById().size());
            metadata.put("feed_spatial_bounds", boundsToMap(bounds));
            metadata.put("changed_from_previous", archiveResult != null && archiveResult.changedFromPrevious());
            metadata.put("error_class", errorClass);
            metadata.put("error_message", errorMessage);
            metadata.put("result", httpResult.isHttpOk() && errorClass == null ? "SUCCESS" : "FAILED");
            jsonOutputService.writeJsonObject(metadataPath, metadata);
            if (httpResult.isHttpOk() && errorClass == null) {
                log.info("Static GTFS准备完成，runId={}，城市={}，feedId={}，routes={}，trips={}，stops={}，stopTimes={}，shapes={}，sha256={}",
                        runId, feed.cityName(), feed.feedId(), staticFeedData.routes().size(), staticFeedData.trips().size(),
                        staticFeedData.stopsCount(), staticFeedData.stopTimesCount(), staticFeedData.shapesById().size(), httpResult.responseSha256());
            } else {
                log.warn("Static GTFS准备未完全成功，runId={}，城市={}，feedId={}，httpStatus={}，errorClass={}，errorMessage={}",
                        runId, feed.cityName(), feed.feedId(), httpResult.statusCode(), errorClass, errorMessage);
            }

            prepared.put(feed.feedId(), new PreparedStaticFeed(staticFeedData, httpResult.responseSha256(), "HTTP_FETCH", metadataPath, bounds, staticShapeQcResult, scheduledTimeResult));
        }
        return prepared;
    }

    private SnapshotProcessResult processRealtimeSnapshot(
            String runId,
            String batchDateText,
            int cycle,
            int snapshotSequence,
            Instant scheduledAt,
            GtfsHttpResult httpResult,
            FeedRunState feedState) throws IOException {
        GtfsFeedDefinition feed = feedState.feed;
        String errorClass = httpResult.errorClass();
        String errorMessage = httpResult.errorMessage();
        String parseStatus = "SKIPPED";
        int entityCount = 0;
        int vehicleCount = 0;
        int uniqueRouteCount = 0;
        int routeResolvedCount = 0;
        int routeUnresolvedCount = 0;
        int routeDirectCount = 0;
        int routeFallbackCount = 0;
        int tripMatchedCount = 0;
        int tripUnmatchedCount = 0;
        int directionMatchCount = 0;
        int directionMismatchCount = 0;
        int directionNotComparableCount = 0;
        int newObservations = 0;
        int duplicateObservations = 0;
        int stationaryCount = 0;
        int staleCount = 0;
        int futureCount = 0;
        int regressionCount = 0;
        int inconsistentCount = 0;
        Path rawPath = null;
        Path parsedPath = null;
        Path enrichedPath = null;
        boolean rawCreated = false;
        boolean parsedCreated = false;
        boolean enrichedCreated = false;
        boolean duplicateSnapshot = false;
        String previousSnapshotSha256 = feedState.lastSuccessfulSnapshotSha;
        String duplicateOfRequestTimestamp = null;
        String duplicateOfRawPath = null;
        List<Map<String, Object>> vehicles = List.of();
        FieldPresenceAudit snapshotAudit = new FieldPresenceAudit.Builder(feed.feedId(), 0).build();
        boolean rawParsedIntegrityOk = true;
        boolean parsedEnrichedIntegrityOk = true;
        boolean entityIdIntegrityOk = true;

        if (httpResult.isHttpOk()) {
            // 成功响应依次完成解析、Static 补全、QC、RAW/JSON 落盘和完整性复核。
            GtfsRealtime.FeedMessage feedMessage = realtimeParser.parse(httpResult.responseBody());
            parseStatus = "PARSED";
            entityCount = feedMessage.getEntityCount();
            snapshotAudit = fieldPresenceAuditService.audit(feed.feedId(), feedMessage);
            EnrichmentResult enrichment = enrichmentService.enrich(runId, feed, feedMessage, feedState.preparedStatic.staticFeedData(), httpResult);
            vehicles = deepCopyVehicles(enrichment.vehicles());
            ObservationCounts observationCounts = annotateVehicles(feedState, vehicles, httpResult.ingestTimestampUtc());
            newObservations = observationCounts.newObservations();
            duplicateObservations = observationCounts.duplicateObservations();
            stationaryCount = observationCounts.stationaryCount();
            staleCount = observationCounts.staleCount();
            futureCount = observationCounts.futureCount();
            regressionCount = observationCounts.regressionCount();
            inconsistentCount = observationCounts.inconsistentCount();
            vehicleCount = enrichment.vehicleCount();
            uniqueRouteCount = enrichment.uniqueRouteCount();
            routeResolvedCount = enrichment.routeResolvedCount();
            routeUnresolvedCount = enrichment.routeUnresolvedCount();
            routeDirectCount = enrichment.routeDirectMatchCount();
            routeFallbackCount = enrichment.routeTripFallbackMatchCount();
            tripMatchedCount = enrichment.tripMatchCount();
            tripUnmatchedCount = enrichment.tripUnmatchedCount();
            directionMatchCount = enrichment.directionMatchCount();
            directionMismatchCount = enrichment.directionMismatchCount();
            directionNotComparableCount = enrichment.directionNotComparableCount();

            duplicateSnapshot = httpResult.responseSha256() != null
                    && feedState.lastSuccessfulSnapshotSha != null
                    && httpResult.responseSha256().equals(feedState.lastSuccessfulSnapshotSha);

            SnapshotArtifacts existingArtifacts = feedState.artifactsBySha.get(httpResult.responseSha256());
            if (existingArtifacts == null) {
                // 首次出现的内容保存完整三件套；相同 SHA 的重复快照复用不可变文件。
                rawPath = workspaceRoot.resolve("raw_data").resolve(batchDateText).resolve(feed.cityFolder())
                        .resolve(feed.filePrefix() + "_gtfs_realtime_" + GtfsTime.formatFileTimestamp(httpResult.responseReceivedAt()) + ".pb");
                parsedPath = BusGpsStorageLayout.dailyJsonRoot(workspaceRoot, batchDateText).resolve(feed.cityFolder())
                        .resolve(feed.filePrefix() + "_gtfs_realtime_parsed_full_" + GtfsTime.formatFileTimestamp(httpResult.responseReceivedAt()) + ".json");
                enrichedPath = BusGpsStorageLayout.dailyJsonRoot(workspaceRoot, batchDateText).resolve(feed.cityFolder())
                        .resolve(feed.filePrefix() + "_gtfs_realtime_enriched_full_" + GtfsTime.formatFileTimestamp(httpResult.responseReceivedAt()) + ".json");
                rawArchiveService.save(rawPath, httpResult.responseBody());
                jsonOutputService.writeJsonString(parsedPath, realtimeParser.toJson(feedMessage));
                jsonOutputService.writeJsonObject(enrichedPath, Map.of("snapshot", enrichment.root().get("snapshot"), "vehicles", vehicles));
                rawCreated = true;
                parsedCreated = true;
                enrichedCreated = true;
                existingArtifacts = new SnapshotArtifacts(rawPath, parsedPath, enrichedPath, httpResult.responseSha256(), entityCount, vehicleCount, entityIds(feedMessage), vehicleEntityIds(vehicles));
                feedState.artifactsBySha.put(httpResult.responseSha256(), existingArtifacts);
            } else {
                rawPath = existingArtifacts.rawPath();
                parsedPath = existingArtifacts.parsedPath();
                enrichedPath = existingArtifacts.enrichedPath();
            }

            duplicateOfRequestTimestamp = duplicateSnapshot && feedState.lastSuccessfulRequestTimestamp != null
                    ? feedState.lastSuccessfulRequestTimestamp.toString()
                    : null;
            duplicateOfRawPath = duplicateSnapshot && feedState.lastSuccessfulRawPath != null
                    ? relativize(feedState.lastSuccessfulRawPath)
                    : null;

            int vehicleEntityCount = (int) feedMessage.getEntityList().stream().filter(GtfsRealtime.FeedEntity::hasVehicle).count();
            rawParsedIntegrityOk = entityCount == feedMessage.getEntityCount();
            parsedEnrichedIntegrityOk = vehicleEntityCount == vehicles.size();
            entityIdIntegrityOk = entityIds(feedMessage).equals(vehicleEntityIds(vehicles));

            feedState.totalVehicleEntitiesSeen += vehicleCount;
            feedState.uniqueVehicleObservations += newObservations;
            feedState.duplicateVehicleObservations += duplicateObservations;
            feedState.stationaryCount += stationaryCount;
            feedState.staleTimestampCount += staleCount;
            feedState.futureTimestampCount += futureCount;
            feedState.timestampRegressionCount += regressionCount;
            feedState.inconsistentTimestampPositionCount += inconsistentCount;
            feedState.routeDirectMatchCount += routeDirectCount;
            feedState.routeTripFallbackMatchCount += routeFallbackCount;
            feedState.routeResolvedCount += routeResolvedCount;
            feedState.routeUnresolvedCount += routeUnresolvedCount;
            feedState.tripMatchedCount += tripMatchedCount;
            feedState.tripUnmatchedCount += tripUnmatchedCount;
            feedState.directionMatchCount += directionMatchCount;
            feedState.directionMismatchCount += directionMismatchCount;
            feedState.directionNotComparableCount += directionNotComparableCount;
            feedState.snapshotShas.add(httpResult.responseSha256());
            feedState.vehicleCountsPerSnapshot.add(vehicleCount);
            feedState.snapshotFieldAudits.add(snapshotAudit);
            mergeAudit(feedState.aggregateFieldCounts, snapshotAudit);
            feedState.aggregateVehicleCount += snapshotAudit.vehicleCount();
            feedState.rawParsedIntegrityPassCount += rawParsedIntegrityOk ? 1 : 0;
            feedState.parsedEnrichedIntegrityPassCount += parsedEnrichedIntegrityOk ? 1 : 0;
            feedState.entityIdIntegrityPassCount += entityIdIntegrityOk ? 1 : 0;
            feedState.lastSuccessfulSnapshotSha = httpResult.responseSha256();
            feedState.lastSuccessfulRequestTimestamp = httpResult.requestStartedAt();
            feedState.lastSuccessfulRawPath = rawPath;
        }

        Path metadataPath = BusGpsStorageLayout.dailyJsonRoot(workspaceRoot, batchDateText).resolve(feed.cityFolder())
                .resolve(feed.filePrefix() + "_gtfs_realtime_metadata_" + GtfsTime.formatFileTimestamp(httpResult.responseReceivedAt()) + ".json");
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("run_id", runId);
        metadata.put("cycle_number", cycle);
        metadata.put("snapshot_sequence", snapshotSequence);
        metadata.put("batch_id", runId);
        metadata.put("city_code", feed.cityCode());
        metadata.put("city_name", feed.cityName());
        metadata.put("feed_id", feed.feedId());
        metadata.put("operator", feed.operator());
        metadata.put("service_type", feed.serviceType());
        metadata.put("request_type", "REALTIME");
        metadata.put("scheduled_at", scheduledAt.toString());
        metadata.put("actual_request_started_at", httpResult.requestStartedAt().toString());
        metadata.put("scheduler_drift_ms", Duration.between(scheduledAt, httpResult.requestStartedAt()).toMillis());
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
        metadata.put("duplicate_snapshot", duplicateSnapshot);
        metadata.put("previous_snapshot_sha256", previousSnapshotSha256);
        metadata.put("duplicate_of_sha256", duplicateSnapshot ? previousSnapshotSha256 : null);
        metadata.put("duplicate_of_request_timestamp", duplicateOfRequestTimestamp);
        metadata.put("duplicate_of_raw_path", duplicateOfRawPath);
        metadata.put("raw_object_created", rawCreated);
        metadata.put("parsed_json_created", parsedCreated);
        metadata.put("enriched_json_created", enrichedCreated);
        metadata.put("raw_pb_path", relativize(rawPath));
        metadata.put("parsed_json_path", relativize(parsedPath));
        metadata.put("enriched_json_path", relativize(enrichedPath));
        metadata.put("parse_status", parseStatus);
        metadata.put("entity_count", entityCount);
        metadata.put("vehicle_count", vehicleCount);
        metadata.put("new_vehicle_observations", newObservations);
        metadata.put("duplicate_vehicle_observations", duplicateObservations);
        metadata.put("stationary_count", stationaryCount);
        metadata.put("stale_timestamp_count", staleCount);
        metadata.put("future_timestamp_count", futureCount);
        metadata.put("timestamp_regression_count", regressionCount);
        metadata.put("inconsistent_timestamp_position_count", inconsistentCount);
        metadata.put("route_direct_match_count", routeDirectCount);
        metadata.put("route_trip_fallback_match_count", routeFallbackCount);
        metadata.put("route_resolved_count", routeResolvedCount);
        metadata.put("route_unresolved_count", routeUnresolvedCount);
        metadata.put("trip_matched_count", tripMatchedCount);
        metadata.put("trip_unmatched_count", tripUnmatchedCount);
        metadata.put("direction_match_count", directionMatchCount);
        metadata.put("direction_mismatch_count", directionMismatchCount);
        metadata.put("direction_not_comparable_count", directionNotComparableCount);
        metadata.put("raw_parse_integrity_ok", rawParsedIntegrityOk);
        metadata.put("parsed_enriched_integrity_ok", parsedEnrichedIntegrityOk);
        metadata.put("entity_id_integrity_ok", entityIdIntegrityOk);
        metadata.put("error_class", errorClass);
        metadata.put("error_message", errorMessage);
        metadata.put("result", httpResult.isHttpOk() && "PARSED".equals(parseStatus) ? "SUCCESS" : "FAILED");
        jsonOutputService.writeJsonObject(metadataPath, metadata);
        feedState.metadataPaths.add(metadataPath);
        if (httpResult.isHttpOk() && "PARSED".equals(parseStatus)) {
            log.info("Realtime快照处理完成，城市={}，feedId={}，runId={}，cycle={}，JSON车辆数={}，新观测数={}，重复观测数={}，GPS未来时间数={}，Static路线未解析数={}，Static班次未匹配数={}，duplicateSnapshot={}",
                    feed.cityName(), feed.feedId(), runId, cycle, vehicleCount, newObservations, duplicateObservations,
                    futureCount, routeUnresolvedCount, tripUnmatchedCount, duplicateSnapshot);
        } else {
            log.warn("Realtime快照处理未成功，城市={}，feedId={}，runId={}，cycle={}，httpStatus={}，parseStatus={}，errorClass={}，errorMessage={}",
                    feed.cityName(), feed.feedId(), runId, cycle, httpResult.statusCode(), parseStatus, errorClass, errorMessage);
        }

        Map<String, Object> snapshotRow = new LinkedHashMap<>();
        snapshotRow.put("run_id", runId);
        snapshotRow.put("cycle", cycle);
        snapshotRow.put("feed_id", feed.feedId());
        snapshotRow.put("scheduled_at", scheduledAt.toString());
        snapshotRow.put("request_started_at", httpResult.requestStartedAt().toString());
        snapshotRow.put("http_status", httpResult.statusCode());
        snapshotRow.put("response_bytes", httpResult.responseBytes());
        snapshotRow.put("response_sha256", httpResult.responseSha256());
        snapshotRow.put("duplicate_snapshot", duplicateSnapshot);
        snapshotRow.put("entity_count", entityCount);
        snapshotRow.put("vehicle_count", vehicleCount);
        snapshotRow.put("unique_routes", uniqueRouteCount);
        snapshotRow.put("new_observations", newObservations);
        snapshotRow.put("duplicate_observations", duplicateObservations);
        snapshotRow.put("stationary_count", stationaryCount);
        snapshotRow.put("stale_count", staleCount);
        snapshotRow.put("future_timestamp_count", futureCount);
        snapshotRow.put("route_resolved_count", routeResolvedCount);
        snapshotRow.put("trip_matched_count", tripMatchedCount);
        snapshotRow.put("scheduler_drift_ms", Duration.between(scheduledAt, httpResult.requestStartedAt()).toMillis());
        return new SnapshotProcessResult(snapshotRow);
    }

    private ObservationCounts annotateVehicles(FeedRunState feedState, List<Map<String, Object>> vehicles, Instant ingestTimestampUtc) {
        // FeedRunState 跨周期保存已见观测和车辆上一位置，用于重复与 GPS 跳点判断。
        int newObservations = 0;
        int duplicateObservations = 0;
        int stationaryCount = 0;
        int staleCount = 0;
        int futureCount = 0;
        int regressionCount = 0;
        int inconsistentCount = 0;

        for (Map<String, Object> vehicle : vehicles) {
            String observationKey = HashUtils.sha256Hex((
                    feedState.feed.feedId() + "|" +
                            string(vehicle.get("entity_id")) + "|" +
                            string(vehicle.get("vehicle_id")) + "|" +
                            string(vehicle.get("trip_id")) + "|" +
                            string(vehicle.get("realtime_route_id")) + "|" +
                            string(vehicle.get("latitude")) + "|" +
                            string(vehicle.get("longitude")) + "|" +
                            string(vehicle.get("vehicle_timestamp"))).getBytes());
            vehicle.put("observation_key", observationKey);

            String vehicleId = stringOrNull(vehicle.get("vehicle_id"));
            Long vehicleTimestamp = toLong(vehicle.get("vehicle_timestamp"));
            Double latitude = toDouble(vehicle.get("latitude"));
            Double longitude = toDouble(vehicle.get("longitude"));
            String observationIdentity = vehicleId != null && vehicleTimestamp != null
                    ? feedState.feed.feedId() + "|" + vehicleId + "|" + vehicleTimestamp
                    : observationKey;
            // 重复观测只做标记并完整保留，绝不在数据层去重或丢弃。
            boolean duplicateObservation = !feedState.seenObservationKeys.add(observationIdentity);
            vehicle.put("duplicate_observation", duplicateObservation);

            LinkedHashSet<String> qcFlags = new LinkedHashSet<>();
            if (duplicateObservation) {
                duplicateObservations++;
                qcFlags.add("DUPLICATE_OBSERVATION");
            } else {
                newObservations++;
            }
            if (vehicleId == null) {
                qcFlags.add("MISSING_VEHICLE_ID");
            }
            if (vehicle.get("trip_id") == null) {
                qcFlags.add("MISSING_TRIP_ID");
            }
            if (vehicle.get("realtime_route_id") == null) {
                qcFlags.add("MISSING_ROUTE_ID");
            }
            if (!Boolean.TRUE.equals(vehicle.get("route_resolved"))) {
                qcFlags.add("ROUTE_UNRESOLVED");
            }
            if (!Boolean.TRUE.equals(vehicle.get("trip_static_matched"))) {
                qcFlags.add("TRIP_UNRESOLVED");
            }

            PositionQcResult positionQc = positionQcService.evaluate(latitude, longitude, feedState.preparedStatic.bounds());
            vehicle.put("position_present", positionQc.positionPresent());
            vehicle.put("position_wgs84_valid", positionQc.positionWgs84Valid());
            vehicle.put("zero_zero_position", positionQc.zeroZeroPosition());
            vehicle.put("feed_bounds_valid", positionQc.feedBoundsValid());
            vehicle.put("position_qc_status", positionQc.positionQcStatus());
            feedState.positionStatusCounts.merge(positionQc.positionQcStatus(), 1, Integer::sum);
            if (!"VALID_POSITION".equals(positionQc.positionQcStatus())) {
                qcFlags.add(positionQc.positionQcStatus());
            }

            Long freshnessSeconds = vehicleTimestamp == null ? null : Duration.between(Instant.ofEpochSecond(vehicleTimestamp), ingestTimestampUtc).getSeconds();
            vehicle.put("freshness_seconds", freshnessSeconds);
            if (freshnessSeconds != null) {
                feedState.freshnessSamples.add(freshnessSeconds);
                if (freshnessSeconds < 0) {
                    futureCount++;
                    qcFlags.add("FUTURE_TIMESTAMP");
                }
            }

            VehicleObservationState previous = vehicleId == null ? null : feedState.lastObservationByVehicleId.get(vehicleId);
            if (previous != null && previous.vehicleTimestamp() != null && vehicleTimestamp != null) {
                if (vehicleTimestamp > previous.vehicleTimestamp()) {
                    feedState.timestampAdvancedCount++;
                    if (samePosition(previous, latitude, longitude)) {
                        stationaryCount++;
                        qcFlags.add("STATIONARY");
                    }
                } else if (vehicleTimestamp.equals(previous.vehicleTimestamp())) {
                    feedState.timestampUnchangedCount++;
                    if (samePosition(previous, latitude, longitude)) {
                        staleCount++;
                        qcFlags.add("STALE_TIMESTAMP");
                    } else {
                        inconsistentCount++;
                        qcFlags.add("INCONSISTENT_TIMESTAMP_POSITION");
                    }
                } else {
                    regressionCount++;
                    qcFlags.add("TIMESTAMP_REGRESSION");
                }
            }

            GpsJumpQcResult jumpQc = gpsJumpQcService.evaluate(
                    previous == null ? null : previous.vehicleTimestamp(),
                    previous == null ? null : previous.latitude(),
                    previous == null ? null : previous.longitude(),
                    previous != null && previous.positionValid(),
                    vehicleTimestamp,
                    latitude,
                    longitude,
                    positionQc.transitPositionValid());
            vehicle.put("previous_vehicle_timestamp", jumpQc.previousVehicleTimestamp());
            vehicle.put("vehicle_gap_seconds", jumpQc.vehicleGapSeconds());
            vehicle.put("gap_status", jumpQc.gapStatus());
            vehicle.put("time_delta_seconds", jumpQc.timeDeltaSeconds());
            vehicle.put("distance_from_previous_m", jumpQc.distanceFromPreviousM());
            vehicle.put("derived_speed_kmh", jumpQc.derivedSpeedKmh());
            vehicle.put("gps_jump_status", jumpQc.gpsJumpStatus());
            vehicle.put("jump_calculation_skipped_reason", jumpQc.jumpCalculationSkippedReason());

            if (jumpQc.vehicleGapSeconds() != null) {
                feedState.gapSamples.add(jumpQc.vehicleGapSeconds());
            }
            if ("LONG_GAP".equals(jumpQc.gapStatus())) {
                qcFlags.add("LONG_GAP");
                feedState.longGapCount++;
            }
            if ("NORMAL".equals(jumpQc.gpsJumpStatus())) {
                feedState.comparableJumpCount++;
            } else if ("SUSPICIOUS_SPEED".equals(jumpQc.gpsJumpStatus())) {
                feedState.comparableJumpCount++;
                feedState.suspiciousSpeedCount++;
                qcFlags.add("SUSPICIOUS_SPEED");
                recordJumpEvent(feedState, vehicle, previous, jumpQc, "SUSPICIOUS_SPEED");
            } else if ("GPS_JUMP".equals(jumpQc.gpsJumpStatus())) {
                feedState.comparableJumpCount++;
                feedState.gpsJumpCount++;
                qcFlags.add("GPS_JUMP");
                recordJumpEvent(feedState, vehicle, previous, jumpQc, "GPS_JUMP");
            }
            if (jumpQc.derivedSpeedKmh() != null) {
                feedState.maxDerivedSpeedKmh = Math.max(feedState.maxDerivedSpeedKmh, jumpQc.derivedSpeedKmh());
            }

            ObservationEligibilityPolicy.Eligibility eligibility = observationEligibilityPolicy.evaluate(
                    qcFlags,
                    positionQc.positionQcStatus(),
                    jumpQc.gpsJumpStatus());
            vehicle.put("analysis_eligible", eligibility.analysisEligible());
            vehicle.put("spatial_eligible", eligibility.spatialEligible());
            vehicle.put("qc_flags", qcFlags.isEmpty() ? List.of("VALID") : List.copyOf(qcFlags));

            if (vehicleId != null) {
                feedState.lastObservationByVehicleId.put(vehicleId, new VehicleObservationState(
                        vehicleId,
                        vehicleTimestamp,
                        latitude,
                        longitude,
                        positionQc.transitPositionValid()));
            }
        }

        return new ObservationCounts(newObservations, duplicateObservations, stationaryCount, staleCount, futureCount, regressionCount, inconsistentCount);
    }

    private MatrixPaths writeFieldPresence(String runId, String batchDateText, Path runReportRoot, Map<String, FeedRunState> feedStates) throws IOException {
        Map<String, String> displayNames = Map.of(
                "mybas-johor", "JB",
                "mybas-kuching", "Kuching",
                "rapid-bus-kl", "KL Bus",
                "rapid-bus-mrtfeeder", "MRT Feeder",
                "mybas-melaka", "Melaka");
        List<FieldPresenceAudit> aggregateAudits = buildAggregateFieldPresenceAudits(feedStates);
        fieldPresenceMatrixService.writeMatrix(BusGpsStorageLayout.dailyJsonRoot(workspaceRoot, batchDateText), runId, aggregateAudits, orderedDisplayNames(displayNames));
        return fieldPresenceMatrixService.writeMatrix(runReportRoot, "field_presence", aggregateAudits, orderedDisplayNames(displayNames));
    }

    private Path writePositionQcSummary(Path runReportRoot, Map<String, FeedRunState> feedStates) throws IOException {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (FeedRunState state : feedStates.values()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("feed_id", state.feed.feedId());
            row.put("observations", state.totalVehicleEntitiesSeen);
            row.put("valid_position", state.positionStatusCounts.getOrDefault("VALID_POSITION", 0));
            row.put("missing_position", state.positionStatusCounts.getOrDefault("MISSING_POSITION", 0));
            row.put("invalid_wgs84", state.positionStatusCounts.getOrDefault("INVALID_WGS84", 0));
            row.put("zero_zero_position", state.positionStatusCounts.getOrDefault("ZERO_ZERO_POSITION", 0));
            row.put("out_of_bounds", state.positionStatusCounts.getOrDefault("OUT_OF_BOUNDS", 0));
            row.put("valid_position_rate", percentage(state.positionStatusCounts.getOrDefault("VALID_POSITION", 0), state.totalVehicleEntitiesSeen));
            rows.add(row);
        }
        Path path = runReportRoot.resolve("gps_position_qc_summary.json");
        jsonOutputService.writeJsonObject(path, rows);
        return path;
    }

    private Path writeGpsJumpEventsCsv(Path runReportRoot, Map<String, FeedRunState> feedStates) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("feed_id,vehicle_id,previous_timestamp,current_timestamp,previous_lat,previous_lon,current_lat,current_lon,time_delta_seconds,distance_meters,derived_speed_kmh,status");
        for (FeedRunState state : feedStates.values()) {
            for (Map<String, Object> row : state.jumpEvents) {
                lines.add(String.join(",",
                        string(row.get("feed_id")),
                        csv(string(row.get("vehicle_id"))),
                        string(row.get("previous_timestamp")),
                        string(row.get("current_timestamp")),
                        string(row.get("previous_lat")),
                        string(row.get("previous_lon")),
                        string(row.get("current_lat")),
                        string(row.get("current_lon")),
                        string(row.get("time_delta_seconds")),
                        string(row.get("distance_meters")),
                        string(row.get("derived_speed_kmh")),
                        string(row.get("status"))));
            }
        }
        Path path = runReportRoot.resolve("gps_jump_events.csv");
        Files.writeString(path, String.join(System.lineSeparator(), lines) + System.lineSeparator());
        return path;
    }

    private Path writeGapFreshnessSummary(Path runReportRoot, Map<String, FeedRunState> feedStates) throws IOException {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (FeedRunState state : feedStates.values()) {
            rows.add(gapFreshnessSummaryService.summarize(state.feed.feedId(), state.freshnessSamples, state.futureTimestampCount, state.gapSamples));
        }
        Path path = runReportRoot.resolve("gap_freshness_summary.json");
        jsonOutputService.writeJsonObject(path, rows);
        return path;
    }

    private Path writeFeedSpatialBounds(Path runReportRoot, Map<String, FeedRunState> feedStates) throws IOException {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (FeedRunState state : feedStates.values()) {
            rows.add(boundsToMap(state.preparedStatic.bounds()));
        }
        Path path = runReportRoot.resolve("feed_spatial_bounds.json");
        jsonOutputService.writeJsonObject(path, rows);
        return path;
    }

    private Path writeStaticShapeQc(Path runReportRoot, Map<String, FeedRunState> feedStates) throws IOException {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (FeedRunState state : feedStates.values()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("summary", state.preparedStatic.staticShapeQcResult().summary());
            row.put("shape_details", state.preparedStatic.staticShapeQcResult().shapeDetails());
            rows.add(row);
        }
        Path path = runReportRoot.resolve("static_shape_qc.json");
        jsonOutputService.writeJsonObject(path, rows);
        return path;
    }

    private Path writeRouteShapeLengthJson(Path runReportRoot, Map<String, FeedRunState> feedStates) throws IOException {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (FeedRunState state : feedStates.values()) {
            rows.addAll(state.preparedStatic.staticShapeQcResult().routeShapeLengths());
        }
        Path path = runReportRoot.resolve("route_shape_length.json");
        jsonOutputService.writeJsonObject(path, rows);
        return path;
    }

    private Path writeRouteShapeLengthCsv(Path runReportRoot, Map<String, FeedRunState> feedStates) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("feed_id,route_id,route_short_name,route_long_name,direction_id,shape_id,shape_length_km,trip_count_using_shape");
        for (FeedRunState state : feedStates.values()) {
            for (Map<String, Object> row : state.preparedStatic.staticShapeQcResult().routeShapeLengths()) {
                lines.add(String.join(",",
                        string(row.get("feed_id")),
                        string(row.get("route_id")),
                        csv(string(row.get("route_short_name"))),
                        csv(string(row.get("route_long_name"))),
                        string(row.get("direction_id")),
                        csv(string(row.get("shape_id"))),
                        string(row.get("shape_length_km")),
                        string(row.get("trip_count_using_shape"))));
            }
        }
        Path path = runReportRoot.resolve("route_shape_length.csv");
        Files.writeString(path, String.join(System.lineSeparator(), lines) + System.lineSeparator());
        return path;
    }

    private Path writeScheduledTripTimeQcJson(Path runReportRoot, Map<String, FeedRunState> feedStates) throws IOException {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (FeedRunState state : feedStates.values()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("summary", state.preparedStatic.scheduledTimeResult().summary());
            row.put("trip_rows", state.preparedStatic.scheduledTimeResult().tripRows());
            rows.add(row);
        }
        Path path = runReportRoot.resolve("scheduled_trip_time_qc.json");
        jsonOutputService.writeJsonObject(path, rows);
        return path;
    }

    private Path writeScheduledTripTimeCsv(Path runReportRoot, Map<String, FeedRunState> feedStates) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("feed_id,route_id,route_short_name,route_long_name,trip_id,direction_id,shape_id,first_stop_sequence,last_stop_sequence,scheduled_start_seconds,scheduled_end_seconds,scheduled_duration_seconds,scheduled_duration_minutes,scheduled_time_qc_status");
        for (FeedRunState state : feedStates.values()) {
            for (Map<String, Object> row : state.preparedStatic.scheduledTimeResult().tripRows()) {
                lines.add(String.join(",",
                        string(row.get("feed_id")),
                        string(row.get("route_id")),
                        csv(string(row.get("route_short_name"))),
                        csv(string(row.get("route_long_name"))),
                        csv(string(row.get("trip_id"))),
                        string(row.get("direction_id")),
                        csv(string(row.get("shape_id"))),
                        string(row.get("first_stop_sequence")),
                        string(row.get("last_stop_sequence")),
                        string(row.get("scheduled_start_seconds")),
                        string(row.get("scheduled_end_seconds")),
                        string(row.get("scheduled_duration_seconds")),
                        string(row.get("scheduled_duration_minutes")),
                        string(row.get("scheduled_time_qc_status"))));
            }
        }
        Path path = runReportRoot.resolve("scheduled_trip_time.csv");
        Files.writeString(path, String.join(System.lineSeparator(), lines) + System.lineSeparator());
        return path;
    }

    private Path writeScheduledRouteTimeSummaryCsv(Path runReportRoot, Map<String, FeedRunState> feedStates) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("feed_id,route_id,route_short_name,route_long_name,direction_id,trip_count,valid_trip_count,invalid_trip_count,valid_rate,duration_p50_min,duration_p90_min,duration_min,duration_max");
        for (FeedRunState state : feedStates.values()) {
            for (Map<String, Object> row : state.preparedStatic.scheduledTimeResult().routeSummaryRows()) {
                lines.add(String.join(",",
                        string(row.get("feed_id")),
                        string(row.get("route_id")),
                        csv(string(row.get("route_short_name"))),
                        csv(string(row.get("route_long_name"))),
                        string(row.get("direction_id")),
                        string(row.get("trip_count")),
                        string(row.get("valid_trip_count")),
                        string(row.get("invalid_trip_count")),
                        string(row.get("valid_rate")),
                        string(row.get("duration_p50_min")),
                        string(row.get("duration_p90_min")),
                        string(row.get("duration_min")),
                        string(row.get("duration_max"))));
            }
        }
        Path path = runReportRoot.resolve("scheduled_route_time_summary.csv");
        Files.writeString(path, String.join(System.lineSeparator(), lines) + System.lineSeparator());
        return path;
    }

    private Path writeManifest(
            Path runReportRoot,
            String batchDateText,
            String runId,
            Instant runStartedAt,
            Instant runFinishedAt,
            int successfulRequests,
            int failedRequests,
            int http429Count,
            int http5xxCount,
            int timeoutCount,
            List<Map<String, Object>> feedSummaries,
            Path positionQcSummaryPath,
            Path gpsJumpEventsPath,
            Path gapFreshnessSummaryPath,
            Path feedSpatialBoundsPath,
            Path staticShapeQcPath,
            Path routeShapeLengthJsonPath,
            Path scheduledTripTimeQcPath) throws IOException {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("run_id", runId);
        manifest.put("timezone", GtfsTime.MALAYSIA_ZONE.getId());
        manifest.put("planned_cycles", plannedCycles);
        manifest.put("completed_cycles", plannedCycles);
        manifest.put("feed_count", 5);
        manifest.put("planned_realtime_requests", 5 * plannedCycles);
        manifest.put("actual_realtime_requests", 5 * plannedCycles);
        manifest.put("successful_requests", successfulRequests);
        manifest.put("failed_requests", failedRequests);
        manifest.put("http_429_count", http429Count);
        manifest.put("http_5xx_count", http5xxCount);
        manifest.put("timeout_count", timeoutCount);
        manifest.put("run_started_at", runStartedAt.toString());
        manifest.put("run_finished_at", runFinishedAt.toString());
        manifest.put("actual_duration_seconds", Duration.between(runStartedAt, runFinishedAt).toSeconds());
        manifest.put("batch_date", batchDateText);
        manifest.put("feeds", feedSummaries);
        manifest.put("reports", Map.of(
                "gps_position_qc_summary", relativize(positionQcSummaryPath),
                "gps_jump_events", relativize(gpsJumpEventsPath),
                "gap_freshness_summary", relativize(gapFreshnessSummaryPath),
                "feed_spatial_bounds", relativize(feedSpatialBoundsPath),
                "static_shape_qc", relativize(staticShapeQcPath),
                "route_shape_length", relativize(routeShapeLengthJsonPath),
                "scheduled_trip_time_qc", relativize(scheduledTripTimeQcPath)));
        Path manifestPath = runReportRoot.resolve(runProfile.manifestFileName);
        jsonOutputService.writeJsonObject(manifestPath, manifest);
        return manifestPath;
    }

    private List<Map<String, Object>> buildFeedSummaries(Map<String, FeedRunState> feedStates) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (FeedRunState state : feedStates.values()) {
            List<Long> intervals = actualIntervals(state.requestStartedAtList);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("feed_id", state.feed.feedId());
            row.put("city", state.feed.cityName());
            row.put("planned_requests", plannedCycles);
            row.put("actual_requests", state.actualRequestCount);
            row.put("successful_requests", state.snapshotShas.size());
            row.put("failed_requests", state.actualRequestCount - state.snapshotShas.size());
            row.put("vehicle_count_per_snapshot", state.vehicleCountsPerSnapshot);
            row.put("snapshot_sha256_per_cycle", state.snapshotShas);
            row.put("duplicate_snapshot_count", duplicateSnapshotCount(state.snapshotShas));
            row.put("unique_snapshot_count", new LinkedHashSet<>(state.snapshotShas).size());
            row.put("total_vehicle_entities_seen", state.totalVehicleEntitiesSeen);
            row.put("unique_vehicle_observations", state.uniqueVehicleObservations);
            row.put("duplicate_vehicle_observations", state.duplicateVehicleObservations);
            row.put("duplicate_observation_rate", percentage(state.duplicateVehicleObservations, state.totalVehicleEntitiesSeen));
            row.put("stationary_count", state.stationaryCount);
            row.put("stale_timestamp_count", state.staleTimestampCount);
            row.put("future_timestamp_count", state.futureTimestampCount);
            row.put("timestamp_regression_count", state.timestampRegressionCount);
            row.put("inconsistent_timestamp_position_count", state.inconsistentTimestampPositionCount);
            row.put("timestamp_advanced_count", state.timestampAdvancedCount);
            row.put("timestamp_unchanged_count", state.timestampUnchangedCount);
            row.put("route_direct_match_count", state.routeDirectMatchCount);
            row.put("route_trip_fallback_match_count", state.routeTripFallbackMatchCount);
            row.put("route_resolved_count", state.routeResolvedCount);
            row.put("route_unresolved_count", state.routeUnresolvedCount);
            row.put("route_resolved_rate", percentage(state.routeResolvedCount, state.totalVehicleEntitiesSeen));
            row.put("trip_matched_count", state.tripMatchedCount);
            row.put("trip_unmatched_count", state.tripUnmatchedCount);
            row.put("trip_match_rate", percentage(state.tripMatchedCount, state.totalVehicleEntitiesSeen));
            row.put("direction_match_count", state.directionMatchCount);
            row.put("direction_mismatch_count", state.directionMismatchCount);
            row.put("direction_not_comparable_count", state.directionNotComparableCount);
            row.put("position_qc_counts", state.positionStatusCounts);
            row.put("comparable_jump_count", state.comparableJumpCount);
            row.put("suspicious_speed_count", state.suspiciousSpeedCount);
            row.put("gps_jump_count", state.gpsJumpCount);
            row.put("long_gap_count", state.longGapCount);
            row.put("max_derived_speed_kmh", round2(state.maxDerivedSpeedKmh));
            row.put("gap_p50_seconds", StatsUtils.percentileLong(state.gapSamples, 50));
            row.put("gap_p90_seconds", StatsUtils.percentileLong(state.gapSamples, 90));
            row.put("gap_p95_seconds", StatsUtils.percentileLong(state.gapSamples, 95));
            row.put("gap_max_seconds", StatsUtils.maxLong(state.gapSamples));
            row.put("freshness_p50_seconds", StatsUtils.percentileLong(state.freshnessSamples, 50));
            row.put("freshness_p90_seconds", StatsUtils.percentileLong(state.freshnessSamples, 90));
            row.put("freshness_p95_seconds", StatsUtils.percentileLong(state.freshnessSamples, 95));
            row.put("freshness_max_seconds", StatsUtils.maxLong(state.freshnessSamples));
            row.put("actual_intervals_seconds", intervals);
            row.put("median_interval_seconds", StatsUtils.percentileLong(intervals, 50));
            row.put("min_interval_seconds", StatsUtils.minLong(intervals));
            row.put("max_interval_seconds", StatsUtils.maxLong(intervals));
            row.put("median_scheduler_drift_ms", StatsUtils.percentileLong(state.schedulerDriftMs, 50));
            row.put("p95_scheduler_drift_ms", StatsUtils.percentileLong(state.schedulerDriftMs, 95));
            row.put("max_scheduler_drift_ms", StatsUtils.maxLong(state.schedulerDriftMs));
            row.put("static_sha256", state.preparedStatic.staticSha256());
            row.put("static_version_source", state.preparedStatic.staticVersionSource());
            row.put("raw_parse_integrity_pass_count", state.rawParsedIntegrityPassCount);
            row.put("parsed_enriched_integrity_pass_count", state.parsedEnrichedIntegrityPassCount);
            row.put("entity_id_integrity_pass_count", state.entityIdIntegrityPassCount);
            rows.add(row);
        }
        return rows;
    }

    private Path writeFeedSummaryCsv(Path runReportRoot, String runId, List<Map<String, Object>> feedSummaries) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("run_id,feed_id,city,planned_requests,actual_requests,successful_requests,failed_requests,unique_snapshot_count,duplicate_snapshot_count,total_vehicle_entities_seen,unique_vehicle_observations,duplicate_vehicle_observations,duplicate_observation_rate,median_interval_seconds,min_interval_seconds,max_interval_seconds,median_scheduler_drift_ms,p95_scheduler_drift_ms,max_scheduler_drift_ms,route_resolved_rate,trip_match_rate,stationary_count,stale_timestamp_count,future_timestamp_count,timestamp_regression_count,valid_position,zero_zero_position,out_of_bounds,comparable_jump_count,suspicious_speed_count,gps_jump_count,long_gap_count,max_derived_speed_kmh");
        for (Map<String, Object> row : feedSummaries) {
            @SuppressWarnings("unchecked")
            Map<String, Integer> positionCounts = (Map<String, Integer>) row.get("position_qc_counts");
            lines.add(String.join(",",
                    runId,
                    string(row.get("feed_id")),
                    csv(string(row.get("city"))),
                    string(row.get("planned_requests")),
                    string(row.get("actual_requests")),
                    string(row.get("successful_requests")),
                    string(row.get("failed_requests")),
                    string(row.get("unique_snapshot_count")),
                    string(row.get("duplicate_snapshot_count")),
                    string(row.get("total_vehicle_entities_seen")),
                    string(row.get("unique_vehicle_observations")),
                    string(row.get("duplicate_vehicle_observations")),
                    string(row.get("duplicate_observation_rate")),
                    string(row.get("median_interval_seconds")),
                    string(row.get("min_interval_seconds")),
                    string(row.get("max_interval_seconds")),
                    string(row.get("median_scheduler_drift_ms")),
                    string(row.get("p95_scheduler_drift_ms")),
                    string(row.get("max_scheduler_drift_ms")),
                    string(row.get("route_resolved_rate")),
                    string(row.get("trip_match_rate")),
                    string(row.get("stationary_count")),
                    string(row.get("stale_timestamp_count")),
                    string(row.get("future_timestamp_count")),
                    string(row.get("timestamp_regression_count")),
                    string(positionCounts.getOrDefault("VALID_POSITION", 0)),
                    string(positionCounts.getOrDefault("ZERO_ZERO_POSITION", 0)),
                    string(positionCounts.getOrDefault("OUT_OF_BOUNDS", 0)),
                    string(row.get("comparable_jump_count")),
                    string(row.get("suspicious_speed_count")),
                    string(row.get("gps_jump_count")),
                    string(row.get("long_gap_count")),
                    string(row.get("max_derived_speed_kmh"))));
        }
        Path csvPath = runReportRoot.resolve(runProfile.feedSummaryFileName);
        Files.writeString(csvPath, String.join(System.lineSeparator(), lines) + System.lineSeparator());
        return csvPath;
    }

    private Path writeSnapshotSummaryCsv(Path runReportRoot, String runId, List<Map<String, Object>> rows) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("run_id,cycle,feed_id,scheduled_at,request_started_at,http_status,response_bytes,response_sha256,duplicate_snapshot,entity_count,vehicle_count,unique_routes,new_observations,duplicate_observations,stationary_count,stale_count,future_timestamp_count,route_resolved_count,trip_matched_count,scheduler_drift_ms");
        for (Map<String, Object> row : rows) {
            lines.add(String.join(",",
                    runId,
                    string(row.get("cycle")),
                    string(row.get("feed_id")),
                    string(row.get("scheduled_at")),
                    string(row.get("request_started_at")),
                    string(row.get("http_status")),
                    string(row.get("response_bytes")),
                    string(row.get("response_sha256")),
                    string(row.get("duplicate_snapshot")),
                    string(row.get("entity_count")),
                    string(row.get("vehicle_count")),
                    string(row.get("unique_routes")),
                    string(row.get("new_observations")),
                    string(row.get("duplicate_observations")),
                    string(row.get("stationary_count")),
                    string(row.get("stale_count")),
                    string(row.get("future_timestamp_count")),
                    string(row.get("route_resolved_count")),
                    string(row.get("trip_matched_count")),
                    string(row.get("scheduler_drift_ms"))));
        }
        Path csvPath = runReportRoot.resolve(runProfile.snapshotSummaryFileName);
        Files.writeString(csvPath, String.join(System.lineSeparator(), lines) + System.lineSeparator());
        return csvPath;
    }

    private List<FieldPresenceAudit> buildAggregateFieldPresenceAudits(Map<String, FeedRunState> feedStates) {
        List<FieldPresenceAudit> audits = new ArrayList<>();
        for (FeedRunState state : feedStates.values()) {
            FieldPresenceAudit.Builder builder = new FieldPresenceAudit.Builder(state.feed.feedId(), state.aggregateVehicleCount);
            for (Map.Entry<String, Integer> entry : state.aggregateFieldCounts.entrySet()) {
                builder.put(entry.getKey(), entry.getValue());
            }
            audits.add(builder.build());
        }
        return audits;
    }

    private void mergeAudit(Map<String, Integer> aggregateFieldCounts, FieldPresenceAudit audit) {
        for (Map.Entry<String, FieldPresenceAudit.FieldPresenceCount> entry : audit.fields().entrySet()) {
            aggregateFieldCounts.merge(entry.getKey(), entry.getValue().presentCount(), Integer::sum);
        }
    }

    private Map<String, Object> boundsToMap(FeedSpatialBounds bounds) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("feed_id", bounds.feedId());
        map.put("total_shape_count", bounds.totalShapeCount());
        map.put("referenced_shape_count", bounds.referencedShapeCount());
        map.put("orphan_shape_count", bounds.orphanShapeCount());
        map.put("referenced_shape_point_count", bounds.referencedShapePointCount());
        map.put("stop_count", bounds.stopCount());
        map.put("bounds_source", bounds.boundsSource());
        map.put("bounds_warning", bounds.boundsWarning());
        map.put("static_sha256", bounds.staticSha256());
        map.put("raw_min_lat", bounds.rawMinLat());
        map.put("raw_max_lat", bounds.rawMaxLat());
        map.put("raw_min_lon", bounds.rawMinLon());
        map.put("raw_max_lon", bounds.rawMaxLon());
        map.put("buffer_km", bounds.bufferKm());
        map.put("effective_min_lat", bounds.effectiveMinLat());
        map.put("effective_max_lat", bounds.effectiveMaxLat());
        map.put("effective_min_lon", bounds.effectiveMinLon());
        map.put("effective_max_lon", bounds.effectiveMaxLon());
        return map;
    }

    private void recordJumpEvent(FeedRunState feedState, Map<String, Object> vehicle, VehicleObservationState previous, GpsJumpQcResult jumpQc, String status) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("feed_id", feedState.feed.feedId());
        row.put("vehicle_id", vehicle.get("vehicle_id"));
        row.put("previous_timestamp", jumpQc.previousVehicleTimestamp());
        row.put("current_timestamp", vehicle.get("vehicle_timestamp"));
        row.put("previous_lat", previous == null ? null : previous.latitude());
        row.put("previous_lon", previous == null ? null : previous.longitude());
        row.put("current_lat", vehicle.get("latitude"));
        row.put("current_lon", vehicle.get("longitude"));
        row.put("time_delta_seconds", jumpQc.timeDeltaSeconds());
        row.put("distance_meters", round2(jumpQc.distanceFromPreviousM()));
        row.put("derived_speed_kmh", round2(jumpQc.derivedSpeedKmh()));
        row.put("status", status);
        feedState.jumpEvents.add(row);
        if (vehicle.get("vehicle_id") != null) {
            feedState.jumpVehicleIds.add(vehicle.get("vehicle_id").toString());
        }
    }

    private void acquireLock(Path lockPath, String runId) {
        // 锁文件阻止第二个采集进程同时写入相同日期目录。
        try {
            Files.createDirectories(lockPath.getParent());
            if (Files.exists(lockPath)) {
                log.error("多城市采集锁冲突，runId={}，lockPath={}", runId, lockPath);
                throw new IllegalStateException("Run lock already exists: " + lockPath);
            }
            Files.writeString(lockPath, runId);
            log.info("多城市采集锁获取成功，runId={}，lockPath={}", runId, lockPath);
        } catch (IOException ex) {
            log.error("多城市采集锁获取失败，runId={}，lockPath={}，错误信息={}", runId, lockPath, ex.getMessage(), ex);
            throw new IllegalStateException("Failed to acquire run lock.", ex);
        }
    }

    private void validateSchedule() {
        if (plannedCycles <= 0) {
            throw new IllegalArgumentException("plannedCycles must be greater than zero.");
        }
        if (feedIntervalSeconds <= 0) {
            throw new IllegalArgumentException("feedIntervalSeconds must be greater than zero.");
        }
        if (feedStaggerSeconds < 0) {
            throw new IllegalArgumentException("feedStaggerSeconds must be zero or greater.");
        }
    }

    private void releaseLock(Path lockPath) {
        try {
            if (Files.deleteIfExists(lockPath)) {
                log.info("多城市采集锁已释放，lockPath={}", lockPath);
            }
        } catch (IOException ex) {
            log.warn("多城市采集锁释放失败，lockPath={}，错误信息={}", lockPath, ex.getMessage());
        }
    }

    private void sleepUntil(Instant scheduledAt) {
        Instant now = Instant.now();
        if (scheduledAt.isAfter(now)) {
            try {
                Thread.sleep(Duration.between(now, scheduledAt).toMillis());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                log.info("等待下一次Realtime绝对调度时间时收到线程中断信号，scheduledAt={}", scheduledAt);
                throw new IllegalStateException("Interrupted while waiting for next scheduled request.", ex);
            }
        }
    }

    private String relativize(Path path) {
        if (path == null) {
            return null;
        }
        return workspaceRoot.relativize(path).toString().replace('\\', '/');
    }

    private Map<String, String> orderedDisplayNames(Map<String, String> displayNames) {
        Map<String, String> ordered = new LinkedHashMap<>();
        ordered.put("mybas-johor", displayNames.get("mybas-johor"));
        ordered.put("mybas-kuching", displayNames.get("mybas-kuching"));
        ordered.put("rapid-bus-kl", displayNames.get("rapid-bus-kl"));
        ordered.put("rapid-bus-mrtfeeder", displayNames.get("rapid-bus-mrtfeeder"));
        ordered.put("mybas-melaka", displayNames.get("mybas-melaka"));
        return ordered;
    }

    private Set<String> entityIds(GtfsRealtime.FeedMessage feedMessage) {
        Set<String> ids = new LinkedHashSet<>();
        for (GtfsRealtime.FeedEntity entity : feedMessage.getEntityList()) {
            if (entity.hasVehicle() && entity.hasId()) {
                ids.add(entity.getId());
            }
        }
        return ids;
    }

    private Set<String> vehicleEntityIds(List<Map<String, Object>> vehicles) {
        Set<String> ids = new LinkedHashSet<>();
        for (Map<String, Object> vehicle : vehicles) {
            if (vehicle.get("entity_id") != null) {
                ids.add(vehicle.get("entity_id").toString());
            }
        }
        return ids;
    }

    private List<Map<String, Object>> deepCopyVehicles(List<Map<String, Object>> vehicles) {
        List<Map<String, Object>> copy = new ArrayList<>();
        for (Map<String, Object> vehicle : vehicles) {
            copy.add(new LinkedHashMap<>(vehicle));
        }
        return copy;
    }

    private boolean samePosition(VehicleObservationState previous, Double latitude, Double longitude) {
        return java.util.Objects.equals(previous.latitude(), latitude)
                && java.util.Objects.equals(previous.longitude(), longitude);
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return null;
        }
        return Long.parseLong(value.toString());
    }

    private Double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return null;
        }
        return Double.parseDouble(value.toString());
    }

    private String stringOrNull(Object value) {
        return value == null || value.toString().isBlank() ? null : value.toString();
    }

    private String string(Object value) {
        return value == null ? "" : value.toString();
    }

    private String csv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String percentage(int numerator, int denominator) {
        if (denominator == 0) {
            return "0.00%";
        }
        return String.format(Locale.ROOT, "%.2f%%", numerator * 100.0d / denominator);
    }

    private List<Long> actualIntervals(List<Instant> requestStartedAtList) {
        if (requestStartedAtList.size() < 2) {
            return List.of();
        }
        List<Long> intervals = new ArrayList<>();
        List<Instant> ordered = requestStartedAtList.stream().sorted(Comparator.naturalOrder()).toList();
        for (int index = 1; index < ordered.size(); index++) {
            intervals.add(Duration.between(ordered.get(index - 1), ordered.get(index)).toSeconds());
        }
        return intervals;
    }

    private int duplicateSnapshotCount(List<String> snapshotShas) {
        if (snapshotShas.size() < 2) {
            return 0;
        }
        int duplicates = 0;
        for (int index = 1; index < snapshotShas.size(); index++) {
            if (java.util.Objects.equals(snapshotShas.get(index - 1), snapshotShas.get(index))) {
                duplicates++;
            }
        }
        return duplicates;
    }

    private Double round2(Double value) {
        if (value == null) {
            return null;
        }
        return Math.round(value * 100.0d) / 100.0d;
    }

    /** 单个 Feed 已准备好的 Static 数据、边界及质检结果。 */
    private record PreparedStaticFeed(
            StaticFeedData staticFeedData,
            String staticSha256,
            String staticVersionSource,
            Path metadataPath,
            FeedSpatialBounds bounds,
            StaticShapeQcResult staticShapeQcResult,
            ScheduledTripTimeValidationResult scheduledTimeResult) {
    }

    /** 单个 Feed 在整个运行期间持续累积的调度、重复和 QC 状态。 */
    private static class FeedRunState {
        private final GtfsFeedDefinition feed;
        private final PreparedStaticFeed preparedStatic;
        private final Map<String, SnapshotArtifacts> artifactsBySha = new LinkedHashMap<>();
        private final Set<String> seenObservationKeys = new LinkedHashSet<>();
        private final Map<String, VehicleObservationState> lastObservationByVehicleId = new LinkedHashMap<>();
        private final List<String> snapshotShas = new ArrayList<>();
        private final List<Integer> vehicleCountsPerSnapshot = new ArrayList<>();
        private final List<Long> schedulerDriftMs = new ArrayList<>();
        private final List<Instant> requestStartedAtList = new ArrayList<>();
        private final List<Path> metadataPaths = new ArrayList<>();
        private final List<FieldPresenceAudit> snapshotFieldAudits = new ArrayList<>();
        private final Map<String, Integer> aggregateFieldCounts = new LinkedHashMap<>();
        private final Map<String, Integer> positionStatusCounts = new LinkedHashMap<>();
        private final List<Long> gapSamples = new ArrayList<>();
        private final List<Long> freshnessSamples = new ArrayList<>();
        private final List<Map<String, Object>> jumpEvents = new ArrayList<>();
        private final Set<String> jumpVehicleIds = new LinkedHashSet<>();
        private int actualRequestCount;
        private int totalVehicleEntitiesSeen;
        private int uniqueVehicleObservations;
        private int duplicateVehicleObservations;
        private int stationaryCount;
        private int staleTimestampCount;
        private int futureTimestampCount;
        private int timestampRegressionCount;
        private int inconsistentTimestampPositionCount;
        private int routeDirectMatchCount;
        private int routeTripFallbackMatchCount;
        private int routeResolvedCount;
        private int routeUnresolvedCount;
        private int tripMatchedCount;
        private int tripUnmatchedCount;
        private int directionMatchCount;
        private int directionMismatchCount;
        private int directionNotComparableCount;
        private int aggregateVehicleCount;
        private int timestampAdvancedCount;
        private int timestampUnchangedCount;
        private int comparableJumpCount;
        private int suspiciousSpeedCount;
        private int gpsJumpCount;
        private int longGapCount;
        private int rawParsedIntegrityPassCount;
        private int parsedEnrichedIntegrityPassCount;
        private int entityIdIntegrityPassCount;
        private double maxDerivedSpeedKmh;
        private String lastSuccessfulSnapshotSha;
        private Instant lastSuccessfulRequestTimestamp;
        private Path lastSuccessfulRawPath;

        private FeedRunState(GtfsFeedDefinition feed, PreparedStaticFeed preparedStatic) {
            this.feed = feed;
            this.preparedStatic = preparedStatic;
        }
    }

    /** 一个 Snapshot 对应的 RAW、parsed 和 enriched 文件证据。 */
    private record SnapshotArtifacts(
            Path rawPath,
            Path parsedPath,
            Path enrichedPath,
            String sha256,
            int entityCount,
            int vehicleCount,
            Set<String> entityIds,
            Set<String> vehicleEntityIds) {
    }

    /** 某车辆上一次有效观测，用于计算时间间隔、距离和跳点。 */
    private record VehicleObservationState(
            String vehicleId,
            Long vehicleTimestamp,
            Double latitude,
            Double longitude,
            boolean positionValid) {
    }

    /** 单个快照的新增、重复及各类时间质量统计。 */
    private record ObservationCounts(
            int newObservations,
            int duplicateObservations,
            int stationaryCount,
            int staleCount,
            int futureCount,
            int regressionCount,
            int inconsistentCount) {
    }

    /** 单次 Realtime 响应处理完成后的快照摘要。 */
    private record SnapshotProcessResult(Map<String, Object> snapshotSummary) {
    }

    /** 短时或一小时运行的周期、错峰和输出命名配置。 */
    private record RunProfile(
            String runIdPrefix,
            String lockFileName,
            String manifestFileName,
            String feedSummaryFileName,
            String snapshotSummaryFileName) {
        private static RunProfile shortRun() {
            return new RunProfile(
                    "short_run",
                    "short_run.lock",
                    "short_run_manifest.json",
                    "short_run_feed_summary.csv",
                    "short_run_snapshot_summary.csv");
        }

        private static RunProfile oneHour() {
            return new RunProfile(
                    "one_hour_run",
                    "one_hour_run.lock",
                    "one_hour_manifest.json",
                    "one_hour_feed_summary.csv",
                    "one_hour_snapshot_summary.csv");
        }

        private String topLevelManifestName(String runId) {
            return runIdPrefix + "_manifest_" + runId + ".json";
        }
    }
}
