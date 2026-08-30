package com.mytransitgps.persistence.service;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.transit.realtime.GtfsRealtime;
import com.mytransitgps.gtfs.client.GtfsApiClient;
import com.mytransitgps.gtfs.client.GtfsHttpResult;
import com.mytransitgps.gtfs.config.GtfsFeedDefinition;
import com.mytransitgps.gtfs.config.GtfsFeedRegistry;
import com.mytransitgps.gtfs.parser.GtfsRealtimeParser;
import com.mytransitgps.gtfs.service.BusGpsStorageLayout;
import com.mytransitgps.gtfs.service.JsonOutputService;
import com.mytransitgps.gtfs.service.RawArchiveService;
import com.mytransitgps.gtfs.service.RealtimeStaticEnrichmentService;
import com.mytransitgps.gtfs.util.GtfsTime;
import com.mytransitgps.persistence.config.MyTransitGpsDatabaseProperties;
import com.mytransitgps.persistence.entity.CoreCollectionRunEntity;
import com.mytransitgps.persistence.entity.CoreGtfsFeedEntity;
import com.mytransitgps.persistence.entity.JbApiRequestLogEntity;
import com.mytransitgps.persistence.model.DatabaseValidationResult;
import com.mytransitgps.persistence.model.JohorCycleEvidence;
import com.mytransitgps.persistence.model.JohorRunResult;
import com.mytransitgps.persistence.model.LocalStaticArtifact;
import com.mytransitgps.persistence.model.PreparedDatabaseStatic;
import com.mytransitgps.persistence.model.SnapshotPersistenceContext;
import com.mytransitgps.persistence.model.SnapshotPersistenceResult;
import com.mytransitgps.persistence.model.SnapshotQcSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 中文名称：Johor Bahru 显式数据库验收运行器。
 *
 * 功能说明：仅在对应开关显式启用时运行 mybas-johor 十分钟或两小时采集。
 * 每轮严格执行 HTTP→RAW→parsed→enriched→关闭文件→磁盘重读→事务入库，并按绝对 T+n×120 秒调度。
 */
@Service
@ConditionalOnProperty(prefix = "mytransitgps.database", name = "enabled", havingValue = "true")
public class JohorDatabaseTenMinuteRunner {

    private static final Logger log = LoggerFactory.getLogger(JohorDatabaseTenMinuteRunner.class);
    private final ObjectMapper objectMapper;
    private final MyTransitGpsDatabaseProperties properties;
    private final DatabaseConnectionService connectionService;
    private final DatabaseFeedService feedService;
    private final JbStaticPersistenceService staticPersistenceService;
    private final CollectionRunService runService;
    private final ApiRequestEvidenceService requestEvidenceService;
    private final RealtimePersistenceService realtimePersistenceService;
    private final DatabaseMetricsService metricsService;
    private final JohorPersistenceValidator validator;

    public JohorDatabaseTenMinuteRunner(ObjectMapper objectMapper, MyTransitGpsDatabaseProperties properties,
                                        DatabaseConnectionService connectionService, DatabaseFeedService feedService,
                                        JbStaticPersistenceService staticPersistenceService, CollectionRunService runService,
                                        ApiRequestEvidenceService requestEvidenceService, RealtimePersistenceService realtimePersistenceService,
                                        DatabaseMetricsService metricsService, JohorPersistenceValidator validator) {
        this.objectMapper = objectMapper; this.properties = properties; this.connectionService = connectionService;
        this.feedService = feedService; this.staticPersistenceService = staticPersistenceService; this.runService = runService;
        this.requestEvidenceService = requestEvidenceService; this.realtimePersistenceService = realtimePersistenceService;
        this.metricsService = metricsService; this.validator = validator;
    }

    public JohorRunResult run(Path workspaceRoot) {
        validateExplicitConfiguration();
        return runInternal(workspaceRoot, properties.dbTest.getCycles(), properties.dbTest.getIntervalSeconds(),
                "DB_TEN_MINUTE", "jb_db10m", "runtime/jb_db_10min.lock");
    }

    public JohorRunResult runTwoHour(Path workspaceRoot) {
        validateTwoHourConfiguration();
        return runInternal(workspaceRoot, properties.longTest.getCycles(), properties.longTest.getIntervalSeconds(),
                "DB_TWO_HOUR", "jb_db2h", "runtime/jb_db_2hour.lock");
    }

    private JohorRunResult runInternal(Path workspaceRoot, int plannedCycles, int intervalSeconds,
                                       String runType, String runPrefix, String lockRelativePath) {
        Path lock = workspaceRoot.resolve(lockRelativePath);
        acquireLock(lock);
        CoreCollectionRunEntity run = null;
        try {
            connectionService.verify();
            CoreGtfsFeedEntity dbFeed = feedService.requireFeed("mybas-johor");
            GtfsFeedDefinition feed = new GtfsFeedRegistry().findByFeedId("mybas-johor").orElseThrow();
            LocalStaticArtifact artifact = new LocalStaticArtifactResolver(objectMapper).resolve(workspaceRoot, feed.feedId());
            PreparedDatabaseStatic prepared = staticPersistenceService.prepare(workspaceRoot, dbFeed.uid, feed.feedId(), artifact);
            Map<String, Object> baseline = metricsService.snapshot();

            Instant plannedStart = Instant.now();
            String runCode = runPrefix + "_" + GtfsTime.formatFileTimestamp(plannedStart);
            run = runService.start(runCode, runType, plannedStart, plannedCycles);
            Path reportRoot = workspaceRoot.resolve("outputs/db_validation").resolve(runCode);
            Files.createDirectories(reportRoot);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(reportRoot.resolve("00_database_baseline.json").toFile(), baseline);

            List<JohorCycleEvidence> cycles = collect(workspaceRoot, run, feed, dbFeed, prepared, plannedStart, plannedCycles, intervalSeconds);
            int successful = (int) cycles.stream().filter(c -> c.httpStatus() == 200 && c.persistenceResult() != null).count();
            int failed = cycles.size() - successful;
            int http429 = (int) cycles.stream().filter(c -> c.httpStatus() == 429).count();
            int http5xx = (int) cycles.stream().filter(c -> c.httpStatus() >= 500 && c.httpStatus() <= 599).count();
            List<Long> drifts = cycles.stream().map(JohorCycleEvidence::schedulerDriftMs).toList();
            boolean warnings = cycles.stream().anyMatch(c -> c.persistenceResult() != null && c.persistenceResult().qcCount() > 0);
            runService.finish(run, cycles.size(), successful, failed, http429, http5xx, 0, drifts, warnings, failed > 0);
            Instant ended = Instant.now();
            log.info("全部计划轮次已完成，立即停止所有Realtime网络请求；后续仅读取本地JSON和PostgreSQL，runUid={}，cycles={}", run.uid, plannedCycles);

            Map<String, Object> after = metricsService.snapshot();
            DatabaseValidationResult validation = validator.validate(dbFeed.uid, run.uid, prepared, cycles, reportRoot, baseline, after, plannedCycles, intervalSeconds);
            if (!validation.passed()) runService.markValidationFailed(run, "disk JSON ↔ PostgreSQL validation failed; mismatches=" + validation.fieldMismatches());
            return new JohorRunResult(run.uid, runCode, run.actualStartTime, ended, prepared, List.copyOf(cycles), reportRoot,
                    validation.passed(), validation.fieldMismatches());
        } catch (Exception ex) {
            if (run != null) runService.markValidationFailed(run, ex.getClass().getSimpleName() + ": " + ex.getMessage());
            log.error("Johor Bahru数据库采集验收运行失败，runType={}，错误信息={}", runType, ex.getMessage(), ex);
            throw ex instanceof RuntimeException runtime ? runtime : new IllegalStateException(ex);
        } finally {
            releaseLock(lock);
        }
    }

    private List<JohorCycleEvidence> collect(Path workspaceRoot, CoreCollectionRunEntity run, GtfsFeedDefinition feed,
                                              CoreGtfsFeedEntity dbFeed, PreparedDatabaseStatic prepared, Instant runStart,
                                              int plannedCycles, int intervalSeconds) throws IOException {
        GtfsApiClient client = new GtfsApiClient(java.time.Clock.systemUTC(), Duration.ZERO);
        GtfsRealtimeParser parser = new GtfsRealtimeParser(); JsonOutputService json = new JsonOutputService();
        RealtimeStaticEnrichmentService enrichment = new RealtimeStaticEnrichmentService(json);
        RawArchiveService rawArchive = new RawArchiveService(); JohorRealtimeQcAnnotator qc = new JohorRealtimeQcAnnotator();
        Map<String, UUID> priorRequestBySha = new HashMap<>(); Map<String, UUID> priorSnapshotBySha = new HashMap<>();
        List<JohorCycleEvidence> evidence = new ArrayList<>();
        String date = GtfsTime.formatBatchDate(GtfsTime.malaysiaToday());
        for (int cycle = 1; cycle <= plannedCycles; cycle++) {
            Instant scheduledAt = runStart.plusSeconds((long) (cycle - 1) * intervalSeconds);
            sleepUntil(scheduledAt);
            log.info("开始调用GTFS Realtime接口下载数据，城市=Johor Bahru，feedId={}，cycle={}，scheduledAt={}",
                    feed.feedId(), cycle, scheduledAt);
            GtfsHttpResult http = client.fetch("REALTIME", feed.realtimeUrl());
            UUID duplicateRequest = http.responseSha256() == null ? null : priorRequestBySha.get(http.responseSha256());
            JbApiRequestLogEntity request = requestEvidenceService.create(dbFeed.uid, run.uid, duplicateRequest, cycle, cycle, scheduledAt, http);
            long drift = Duration.between(scheduledAt, http.requestStartedAt()).toMillis();
            Path rawPath = null; Path parsedPath = null; Path enrichedPath = null; UUID snapshotUid = null;
            SnapshotPersistenceResult persistence = null; int entityCount = 0; int vehicleCount = 0;
            try {
                if (!http.isHttpOk()) {
                    requestEvidenceService.markFilesAndParse(request, "SKIPPED", 0, 0, workspaceRoot, null, null, null,
                            false, "HTTP_FAILED", null);
                } else {
                    String stamp = GtfsTime.formatFileTimestamp(http.responseReceivedAt());
                    String occurrence = stamp + "_cycle" + cycle + "_" + request.uid;
                    rawPath = workspaceRoot.resolve("raw_data").resolve(date).resolve("Johor_Bahru")
                            .resolve("mybas-johor_gtfs_realtime_raw_" + occurrence + ".pb");
                    parsedPath = BusGpsStorageLayout.dailyJsonRoot(workspaceRoot, date).resolve("Johor_Bahru")
                            .resolve("mybas-johor_gtfs_realtime_parsed_full_" + occurrence + ".json");
                    enrichedPath = BusGpsStorageLayout.dailyJsonRoot(workspaceRoot, date).resolve("Johor_Bahru")
                            .resolve("mybas-johor_gtfs_realtime_enriched_full_" + occurrence + ".json");
                    rawArchive.save(rawPath, http.responseBody());
                    GtfsRealtime.FeedMessage message = parser.parse(Files.readAllBytes(rawPath));
                    entityCount = message.getEntityCount();
                    json.writeJsonString(parsedPath, parser.toJson(message));
                    var enriched = enrichment.enrich(run.runCode, feed, message, prepared.feedData(), http);
                    snapshotUid = UUID.randomUUID();
                    SnapshotQcSummary qcSummary = qc.annotate(feed.feedId(), snapshotUid.toString(), enriched.vehicles(), http.ingestTimestampUtc(), prepared.bounds());
                    vehicleCount = enriched.vehicleCount();
                    json.writeJsonObject(enrichedPath, enriched.root());
                    JsonNode diskRoot = objectMapper.readTree(enrichedPath.toFile());
                    if (diskRoot.path("vehicles").size() != vehicleCount) throw new IllegalStateException("磁盘重读车辆数与解析结果不一致");
                    UUID referencedSnapshot = http.responseSha256() == null ? null : priorSnapshotBySha.get(http.responseSha256());
                    SnapshotPersistenceContext context = new SnapshotPersistenceContext(dbFeed.uid, run.uid, request.uid,
                            snapshotUid, referencedSnapshot, referencedSnapshot != null, prepared.references());
                    persistence = realtimePersistenceService.persist(diskRoot, context);
                    requestEvidenceService.markFilesAndParse(request, "PARSED", entityCount, vehicleCount, workspaceRoot,
                            rawPath, parsedPath, enrichedPath, referencedSnapshot != null, "SUCCESS", null);
                    if (http.responseSha256() != null) {
                        priorRequestBySha.putIfAbsent(http.responseSha256(), request.uid);
                        priorSnapshotBySha.putIfAbsent(http.responseSha256(), snapshotUid);
                    }
                    log.info("Snapshot处理完成，cycle={}，snapshotUid={}，entityCount={}，vehicleCount={}，qcFlags={}，persistenceMs={}",
                            cycle, snapshotUid, entityCount, vehicleCount, qcSummary.qcFlagCount(), persistence.persistenceElapsedMs());
                }
            } catch (Exception ex) {
                requestEvidenceService.markFilesAndParse(request, "FAILED", entityCount, vehicleCount, workspaceRoot,
                        rawPath, parsedPath, enrichedPath, duplicateRequest != null, "PROCESSING_FAILED", ex);
                if (persistence == null) requestEvidenceService.markDatabaseFailure(request, ex);
                log.error("单轮采集处理失败，cycle={}，requestUid={}，错误信息={}", cycle, request.uid, ex.getMessage(), ex);
            }
            Path metadataPath = BusGpsStorageLayout.dailyJsonRoot(workspaceRoot, date).resolve("Johor_Bahru")
                    .resolve("mybas-johor_gtfs_realtime_metadata_" + GtfsTime.formatFileTimestamp(http.responseReceivedAt())
                            + "_cycle" + cycle + "_" + request.uid + ".json");
            Map<String,Object> metadata = new java.util.LinkedHashMap<>();
            metadata.put("run_id", run.runCode); metadata.put("run_uid", run.uid); metadata.put("feed_id", feed.feedId());
            metadata.put("request_uid", request.uid); metadata.put("snapshot_uid", snapshotUid); metadata.put("cycle", cycle);
            metadata.put("scheduled_at", scheduledAt.toString()); metadata.put("request_started_at", http.requestStartedAt().toString());
            metadata.put("response_received_at", http.responseReceivedAt().toString()); metadata.put("http_status", http.statusCode());
            metadata.put("response_bytes", http.responseBytes()); metadata.put("response_sha256", http.responseSha256());
            metadata.put("raw_pb_path", relative(workspaceRoot, rawPath)); metadata.put("parsed_json_path", relative(workspaceRoot, parsedPath));
            metadata.put("enriched_json_path", relative(workspaceRoot, enrichedPath)); metadata.put("entity_count", entityCount);
            metadata.put("vehicle_count", vehicleCount); metadata.put("result", persistence != null ? "SUCCESS" : "FAILED");
            json.writeJsonObject(metadataPath, metadata);
            evidence.add(new JohorCycleEvidence(cycle, request.uid, snapshotUid, scheduledAt, drift, http.statusCode(),
                    rawPath, parsedPath, enrichedPath, http.responseSha256(), entityCount, vehicleCount, persistence));
        }
        return evidence;
    }

    private void validateExplicitConfiguration() {
        if (!properties.dbTest.isEnabled()) throw new IllegalStateException("db-test.enabled is false; explicit enable is required.");
        if (!"jb".equals(properties.database.getSchema()) || !"mybas-johor".equals(properties.database.getFeedId())
                || !"mybas-johor".equals(properties.dbTest.getFeedId())) throw new IllegalStateException("Only schema jb / feed mybas-johor is permitted.");
        if (properties.dbTest.getCycles() != 5 || properties.dbTest.getIntervalSeconds() != 120)
            throw new IllegalStateException("Acceptance run requires exactly 5 cycles at 120-second intervals.");
    }

    private void validateTwoHourConfiguration() {
        if (!properties.longTest.isEnabled()) throw new IllegalStateException("long-test.enabled is false; explicit enable is required.");
        if (!"jb".equals(properties.database.getSchema()) || !"mybas-johor".equals(properties.database.getFeedId())
                || !"mybas-johor".equals(properties.longTest.getFeedId())) throw new IllegalStateException("Only schema jb / feed mybas-johor is permitted.");
        if (properties.longTest.getDurationMinutes() != 120 || properties.longTest.getCycles() != 60 || properties.longTest.getIntervalSeconds() != 120)
            throw new IllegalStateException("Two-hour run requires exactly 60 cycles at 120-second intervals.");
    }

    private String relative(Path root, Path path) {
        return path == null ? null : root.relativize(path).toString().replace('\\', '/');
    }

    private void sleepUntil(Instant scheduledAt) {
        while (Instant.now().isBefore(scheduledAt)) {
            long remaining = Duration.between(Instant.now(), scheduledAt).toMillis();
            try { Thread.sleep(Math.min(remaining, 30_000L)); }
            catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IllegalStateException("绝对调度等待被中断", ex); }
        }
    }

    private void acquireLock(Path lock) {
        try { Files.createDirectories(lock.getParent()); Files.createFile(lock); Files.writeString(lock, "started=" + Instant.now()); }
        catch (FileAlreadyExistsException ex) { log.warn("检测到已有十分钟数据库测试锁，拒绝第二实例，lock={}", lock); throw new IllegalStateException("Johor DB test is already running: " + lock); }
        catch (IOException ex) { throw new IllegalStateException("Cannot create Johor DB test lock.", ex); }
    }

    private void releaseLock(Path lock) {
        try { Files.deleteIfExists(lock); }
        catch (IOException ex) { log.error("十分钟数据库测试锁释放失败，lock={}，错误信息={}", lock, ex.getMessage(), ex); }
    }
}
