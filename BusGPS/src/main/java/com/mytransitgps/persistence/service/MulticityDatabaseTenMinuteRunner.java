package com.mytransitgps.persistence.service;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.transit.realtime.GtfsRealtime;
import com.mytransitgps.gtfs.client.GtfsApiClient;
import com.mytransitgps.gtfs.client.GtfsHttpResult;
import com.mytransitgps.gtfs.config.GtfsFeedDefinition;
import com.mytransitgps.gtfs.parser.GtfsRealtimeParser;
import com.mytransitgps.gtfs.service.BusGpsStorageLayout;
import com.mytransitgps.gtfs.service.JsonOutputService;
import com.mytransitgps.gtfs.service.RawArchiveService;
import com.mytransitgps.gtfs.service.RealtimeStaticEnrichmentService;
import com.mytransitgps.gtfs.util.GtfsTime;
import com.mytransitgps.persistence.config.MyTransitGpsDatabaseProperties;
import com.mytransitgps.persistence.entity.CoreCollectionRunEntity;
import com.mytransitgps.persistence.entity.JbApiRequestLogEntity;
import com.mytransitgps.persistence.model.DatabaseFeedContext;
import com.mytransitgps.persistence.model.LocalStaticArtifact;
import com.mytransitgps.persistence.model.MulticityCycleEvidence;
import com.mytransitgps.persistence.model.MulticityRunResult;
import com.mytransitgps.persistence.model.PreparedDatabaseStatic;
import com.mytransitgps.persistence.model.SnapshotPersistenceContext;
import com.mytransitgps.persistence.model.SnapshotPersistenceResult;
import com.mytransitgps.persistence.model.SnapshotQcSummary;
import com.mytransitgps.persistence.routing.CitySchemaRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 中文名称：五 Feed 磁盘 JSON 到 PostgreSQL 验收运行器。
 *
 * 功能：复用现有下载、Protobuf、Static enrichment、QC 与文件归档链路，按数据库错峰配置执行。
 * 显式验收仍支持固定轮次档位；正式连续模式使用无固定时长Run，持续到应用Stop。
 * 输入为真实DB Feed配置和本地Static，输出为完整磁盘与数据库证据。
 */
@Service
@ConditionalOnProperty(prefix = "mytransitgps.database", name = "enabled", havingValue = "true")
public class MulticityDatabaseTenMinuteRunner {

    private static final Logger log = LoggerFactory.getLogger(MulticityDatabaseTenMinuteRunner.class);
    private final ObjectMapper objectMapper;
    private final MyTransitGpsDatabaseProperties properties;
    private final DatabaseConnectionService connectionService;
    private final DatabaseFeedService feedService;
    private final JbStaticPersistenceService staticPersistenceService;
    private final CollectionRunService runService;
    private final ApiRequestEvidenceService requestEvidenceService;
    private final RealtimePersistenceService realtimePersistenceService;
    private final CitySchemaRouter schemaRouter;
    private final SchemaFingerprintService fingerprintService;
    private final JdbcTemplate jdbc;
    private final MulticityPersistenceValidator validator;
    private final String datasourcePassword;
    private final Map<String, FeedState> continuousStates = new HashMap<>();

    public MulticityDatabaseTenMinuteRunner(ObjectMapper objectMapper, MyTransitGpsDatabaseProperties properties,
                                            DatabaseConnectionService connectionService, DatabaseFeedService feedService,
                                            JbStaticPersistenceService staticPersistenceService, CollectionRunService runService,
                                            ApiRequestEvidenceService requestEvidenceService,
                                            RealtimePersistenceService realtimePersistenceService,
                                            CitySchemaRouter schemaRouter, SchemaFingerprintService fingerprintService,
                                            JdbcTemplate jdbc, MulticityPersistenceValidator validator,
                                            @Value("${spring.datasource.password:}") String datasourcePassword) {
        this.objectMapper = objectMapper; this.properties = properties; this.connectionService = connectionService;
        this.feedService = feedService; this.staticPersistenceService = staticPersistenceService; this.runService = runService;
        this.requestEvidenceService = requestEvidenceService; this.realtimePersistenceService = realtimePersistenceService;
        this.schemaRouter = schemaRouter; this.fingerprintService = fingerprintService; this.jdbc = jdbc;
        this.validator = validator; this.datasourcePassword = datasourcePassword;
    }

    public MulticityRunResult run(Path workspaceRoot) {
        RunProfile profile = validateExplicitConfiguration();
        return runProfile(workspaceRoot, profile, properties.multicityDbTest.isValidateAfterRun(), new HashMap<>());
    }

    public MulticityRunResult runTwoHour(Path workspaceRoot, boolean validateAfterRun) {
        // IDEA 长期模式复用同一套运行器，但以 60 轮 × 120 秒构成一个两小时窗口。
        validateRuntimeEnvironment();
        return runProfile(workspaceRoot, twoHourProfile(), validateAfterRun, continuousStates);
    }

    /**
     * 正式无固定时长连续采集。该方法不设置2小时或其他结束时长；五个Feed按120秒绝对调度持续执行，
     * 只有应用Stop/线程中断才正常收尾，系统级异常则保留证据并由生命周期决定是否重启。
     */
    public void runContinuous(Path workspaceRoot) {
        validateRuntimeEnvironment();
        Path lock = workspaceRoot.resolve("runtime").resolve("multicity_continuous.lock");
        log.info("无固定时长多城市采集准备开始，intervalSeconds={}，feedCount={}，workspaceRoot={}",
                properties.multicityDbTest.getIntervalSeconds(), properties.multicityDbTest.getFeeds().size(),
                workspaceRoot.toAbsolutePath().normalize());
        log.info("准备获取无固定时长采集运行锁，lockName={}", lock.getFileName());
        acquireLock(lock);
        log.info("无固定时长采集运行锁获取成功，lockName={}", lock.getFileName());

        CoreCollectionRunEntity run = null;
        ContinuousStats stats = new ContinuousStats();
        try {
            connectionService.verify();
            List<DatabaseFeedContext> feeds = feedService.requireContexts(properties.multicityDbTest.getFeeds());
            validateFeedMapping(feeds);
            log.info("无固定时长采集Feed配置验证完成，feedCount={}，intervalSeconds={}",
                    feeds.size(), properties.multicityDbTest.getIntervalSeconds());
            Map<String, PreparedDatabaseStatic> prepared = prepareStatic(workspaceRoot, feeds);
            Instant startedAt = Instant.now();
            String runCode = "multicity_continuous_" + GtfsTime.formatFileTimestamp(startedAt);
            run = runService.startContinuousMulticity(runCode, startedAt, feeds.size());
            Path reportRoot = workspaceRoot.resolve("outputs/db_validation").resolve(runCode);
            Files.createDirectories(reportRoot);
            Map<String, Object> beforeFingerprint = fingerprintService.capture();
            writeJson(reportRoot.resolve("01_database_baseline.json"), databaseBaseline(run.uid, feeds, beforeFingerprint));

            feeds.forEach(f -> continuousStates.computeIfAbsent(f.feedId(), ignored -> new FeedState()));
            CollectorComponents components = newCollectorComponents();
            List<DatabaseFeedContext> orderedFeeds = feeds.stream()
                    .sorted(Comparator.comparingInt(DatabaseFeedContext::staggerOffsetSeconds))
                    .toList();
            int intervalSeconds = properties.multicityDbTest.getIntervalSeconds();

            log.info("无固定时长多城市采集Run已启动，runCode={}，runUid={}，feedCount={}，intervalSeconds={}，plannedEnd=NONE",
                    runCode, run.uid, feeds.size(), intervalSeconds);

            int cycle = 0;
            int requestSequence = 0;
            while (!Thread.currentThread().isInterrupted()) {
                cycle++;
                Instant cycleBase = startedAt.plusSeconds((long) (cycle - 1) * intervalSeconds);
                for (DatabaseFeedContext feed : orderedFeeds) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new IllegalStateException("连续采集收到停止信号", new InterruptedException("collector interrupted"));
                    }
                    requestSequence++;
                    Event event = new Event(feed, cycle, cycleBase.plusSeconds(feed.staggerOffsetSeconds()));
                    MulticityCycleEvidence evidence = collectEvent(workspaceRoot, run, prepared, event, requestSequence,
                            continuousStates, components);
                    stats.accept(evidence);
                }
                stats.completedCycles = cycle;
                runService.updateContinuousMulticityProgress(run, stats.completedCycles, stats.actualRequests,
                        stats.successful, stats.failed, stats.http429, stats.http5xx, stats.timeouts, stats.driftMaxMs);
                if (cycle == 1 || cycle % 30 == 0) {
                    log.info("无固定时长连续采集运行中，runCode={}，runUid={}，完整轮次={}，请求={}，成功={}，失败={}，observations={}，qcRows={}",
                            runCode, run.uid, cycle, stats.actualRequests, stats.successful, stats.failed,
                            stats.observations, stats.qcRows);
                }
            }

            runService.finishContinuousMulticity(run, stats.completedCycles, stats.actualRequests, stats.successful,
                    stats.failed, stats.http429, stats.http5xx, stats.timeouts, stats.driftMaxMs);
        } catch (Exception ex) {
            if (run != null) {
                if (isInterruption(ex)) {
                    runService.finishContinuousMulticity(run, stats.completedCycles, stats.actualRequests, stats.successful,
                            stats.failed, stats.http429, stats.http5xx, stats.timeouts, stats.driftMaxMs);
                    log.info("无固定时长多城市采集收到正常停止信号，runCode={}，runUid={}，完整轮次={}，请求={}，证据已保留",
                            run.runCode, run.uid, stats.completedCycles, stats.actualRequests);
                    return;
                }
                runService.markInterruptedMulticity(run, ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
            log.error("无固定时长多城市采集Run失败，runUid={}，错误信息={}",
                    run == null ? null : run.uid, ex.getMessage(), ex);
            throw ex instanceof RuntimeException runtime ? runtime : new IllegalStateException(ex);
        } finally {
            releaseLock(lock);
        }
    }

    private MulticityRunResult runProfile(Path workspaceRoot, RunProfile profile, boolean validateAfterRun,
                                          Map<String, FeedState> feedStates) {
        log.info("多城市采集运行准备开始，runType={}，cyclesPerFeed={}，intervalSeconds={}，expectedAttempts={}，validationEnabled={}，workspaceRoot={}",
                profile.runType(), profile.cyclesPerFeed(), profile.intervalSeconds(), profile.expectedAttempts(),
                validateAfterRun, workspaceRoot.toAbsolutePath().normalize());
        Path lock = workspaceRoot.resolve("runtime").resolve(profile.lockName());
        // 锁必须在连接数据库之前取得，避免两个实例创建相互交错的 Run 和文件。
        log.info("准备获取多城市采集运行锁，lockName={}", lock.getFileName());
        acquireLock(lock);
        log.info("多城市采集运行锁获取成功，lockName={}", lock.getFileName());
        CoreCollectionRunEntity run = null;
        try {
            log.info("开始执行多城市采集数据库连接检查，runType={}", profile.runType());
            connectionService.verify();
            log.info("多城市采集数据库连接检查完成，runType={}", profile.runType());
            log.info("开始验证多城市Feed数据库配置，expectedFeedCount={}", properties.multicityDbTest.getFeeds().size());
            List<DatabaseFeedContext> feeds = feedService.requireContexts(properties.multicityDbTest.getFeeds());
            validateFeedMapping(feeds);
            log.info("多城市Feed数据库配置验证完成，feedCount={}", feeds.size());
            log.info("开始准备全部Feed的Static GTFS，feedCount={}", feeds.size());
            Map<String, PreparedDatabaseStatic> prepared = prepareStatic(workspaceRoot, feeds);
            log.info("全部Feed的Static GTFS准备完成，feedCount={}", prepared.size());
            Instant plannedStart = Instant.now();
            String runCode = profile.runCodePrefix() + GtfsTime.formatFileTimestamp(plannedStart);
            run = runService.startMulticity(runCode, profile.runType(), plannedStart, feeds.size(),
                    profile.cyclesPerFeed(), profile.remarks());
            log.info("多城市采集运行开始，runType={}，runCode={}，runUid={}，feedCount={}，cyclesPerFeed={}，intervalSeconds={}，plannedAttempts={}",
                    profile.runType(), runCode, run.uid, feeds.size(), profile.cyclesPerFeed(),
                    profile.intervalSeconds(), profile.expectedAttempts());
            Path reportRoot = workspaceRoot.resolve("outputs/db_validation").resolve(runCode);
            Files.createDirectories(reportRoot);
            // 采集前后比较 Schema 指纹，保证数据流程没有意外执行 DDL。
            Map<String, Object> beforeFingerprint = fingerprintService.capture();
            writeJson(reportRoot.resolve("01_database_baseline.json"), databaseBaseline(run.uid, feeds, beforeFingerprint));

            List<MulticityCycleEvidence> attempts = collect(workspaceRoot, run, feeds, prepared, plannedStart,
                    profile.cyclesPerFeed(), profile.intervalSeconds(), feedStates);
            long observationCount = attempts.stream().filter(a -> a.persistenceResult() != null)
                    .mapToLong(a -> a.persistenceResult().observationCount()).sum();
            long qcRowCount = attempts.stream().filter(a -> a.persistenceResult() != null)
                    .mapToLong(a -> a.persistenceResult().qcCount()).sum();
            int successful = (int) attempts.stream().filter(a -> a.persistenceResult() != null).count();
            int failed = attempts.size() - successful;
            int http429 = (int) attempts.stream().filter(a -> a.httpStatus() == 429).count();
            int http5xx = (int) attempts.stream().filter(a -> a.httpStatus() >= 500 && a.httpStatus() <= 599).count();
            int timeouts = (int) attempts.stream().filter(a -> "HttpTimeoutException".equals(a.errorClass())).count();
            List<Long> drifts = attempts.stream().map(MulticityCycleEvidence::schedulerDriftMs).toList();
            boolean warnings = attempts.stream().anyMatch(a -> a.persistenceResult() != null && a.persistenceResult().qcCount() > 0);
            runService.finishMulticity(run, profile.cyclesPerFeed(), attempts.size(), successful, failed,
                    http429, http5xx, timeouts, drifts, warnings,
                    failed > 0 || attempts.size() != profile.expectedAttempts());
            log.info("五个Feed均已达到计划Attempt，立即停止Realtime网络请求；每Feed轮次={}，总Attempt={}，后续仅读取磁盘JSON和PostgreSQL，runUid={}",
                    profile.cyclesPerFeed(), attempts.size(), run.uid);
            log.info("多城市采集阶段完成，runCode={}，runUid={}，attempts={}，successful={}，failed={}，observations={}，qcRows={}",
                    runCode, run.uid, attempts.size(), successful, failed, observationCount, qcRowCount);

            Map<String, Object> afterFingerprint = fingerprintService.capture();
            boolean schemaUnchanged = beforeFingerprint.get("sha256").equals(afterFingerprint.get("sha256"));
            Map<String, Object> collectionEvidence = new LinkedHashMap<>();
            collectionEvidence.put("run_uid", run.uid.toString());
            collectionEvidence.put("run_code", runCode);
            collectionEvidence.put("run_type", profile.runType());
            collectionEvidence.put("cycles_per_feed", profile.cyclesPerFeed());
            collectionEvidence.put("interval_seconds", profile.intervalSeconds());
            collectionEvidence.put("planned_attempts", profile.expectedAttempts());
            collectionEvidence.put("attempts", attempts.size());
            collectionEvidence.put("successful_persistence", successful);
            collectionEvidence.put("failed", failed);
            collectionEvidence.put("schema_fingerprint_before", beforeFingerprint);
            collectionEvidence.put("schema_fingerprint_after", afterFingerprint);
            collectionEvidence.put("schema_changed", !schemaUnchanged);
            writeJson(reportRoot.resolve("_collection_evidence.json"), collectionEvidence);
            long fieldMismatches = 0L;
            boolean validationPassed = true;
            if (validateAfterRun) {
                // 长期模式默认延后逐字段重放，避免大报告阻塞下一个 120 秒采集周期。
                log.info("开始多城市数据库完整性验证，runCode={}，runUid={}，attempts={}",
                        runCode, run.uid, attempts.size());
                var validation = validator.validate(run.uid, runCode, feeds, prepared, attempts, reportRoot,
                        beforeFingerprint, afterFingerprint);
                validationPassed = validation.passed();
                fieldMismatches = validation.fieldMismatches();
                if (validationPassed) {
                    log.info("多城市数据库完整性验证完成，runCode={}，runUid={}，fieldMismatches=0，status=PASS",
                            runCode, run.uid);
                } else {
                    log.error("多城市数据库完整性验证未通过，runCode={}，runUid={}，fieldMismatches={}",
                            runCode, run.uid, fieldMismatches);
                }
            }
            boolean collectionPassed = attempts.size() == profile.expectedAttempts()
                    && failed == 0 && schemaUnchanged && validationPassed;
            if (collectionPassed && validateAfterRun) {
                runService.markValidationPassed(run, warnings);
            } else if (collectionPassed) {
                runService.markCollectionPassed(run, warnings,
                        "Continuous collection window complete; full field replay deferred");
            } else {
                runService.markValidationFailed(run, "Part 2C multicity collection/validation failed; mismatches=" + fieldMismatches);
            }
            long durationMs = Duration.between(plannedStart, Instant.now()).toMillis();
            if (collectionPassed) {
                log.info("多城市采集运行完成，runCode={}，runUid={}，actualAttempts={}，successful={}，failed={}，observations={}，qcRows={}，durationMs={}，status={}",
                        runCode, run.uid, attempts.size(), successful, failed, observationCount, qcRowCount,
                        durationMs, warnings ? "SUCCESS_WITH_WARNINGS" : "SUCCESS");
            } else {
                log.error("多城市采集运行未通过，runCode={}，runUid={}，actualAttempts={}，successful={}，failed={}，fieldMismatches={}，durationMs={}",
                        runCode, run.uid, attempts.size(), successful, failed, fieldMismatches, durationMs);
            }
            return new MulticityRunResult(run.uid, runCode, run.actualStartTime, Instant.now(), List.copyOf(attempts),
                    reportRoot, collectionPassed, fieldMismatches);
        } catch (Exception ex) {
            if (run != null) {
                // 即使 IDEA 被停止，也按已写请求日志回填实际数量，禁止遗留 RUNNING 状态。
                runService.markInterruptedMulticity(run, ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
            if (isInterruption(ex)) {
                log.info("多城市数据库运行收到正常停止信号，档位={}，已保留部分证据并安全收尾", profile.runType());
            } else {
                log.error("多城市数据库验收运行失败，档位={}，错误信息={}", profile.runType(), ex.getMessage(), ex);
            }
            throw ex instanceof RuntimeException runtime ? runtime : new IllegalStateException(ex);
        } finally {
            releaseLock(lock);
        }
    }

    private Map<String, PreparedDatabaseStatic> prepareStatic(Path workspaceRoot, List<DatabaseFeedContext> feeds) {
        Map<String, PreparedDatabaseStatic> prepared = new LinkedHashMap<>();
        LocalStaticArtifactResolver resolver = new LocalStaticArtifactResolver(objectMapper);
        for (DatabaseFeedContext feed : feeds) {
            log.info("开始准备Static GTFS，feedId={}，schema={}", feed.feedId(), feed.schemaName());
            LocalStaticArtifact artifact = resolver.resolve(workspaceRoot, feed.feedId());
            PreparedDatabaseStatic result = schemaRouter.withSchema(feed.schemaName(),
                    () -> staticPersistenceService.prepare(workspaceRoot, feed.feedUid(), feed.feedId(), artifact));
            prepared.put(feed.feedId(), result);
            log.info("Static GTFS准备完成，feedId={}，schema={}，staticVersionUid={}，routes={}，trips={}，stops={}，shapes={}",
                    feed.feedId(), feed.schemaName(), result.references().staticVersionUid(), result.references().routeUids().size(),
                    result.references().tripUids().size(), result.references().stopUids().size(),
                    result.references().shapeUids().size());
        }
        return Map.copyOf(prepared);
    }

    /** 单个Feed某一周期的绝对调度事件。 */
    private record Event(DatabaseFeedContext feed, int cycle, Instant scheduledAt) { }

    /** 单次运行复用的HTTP/解析/文件组件，避免每个请求重复创建。 */
    private record CollectorComponents(GtfsApiClient client, GtfsRealtimeParser parser, JsonOutputService json,
                                       RealtimeStaticEnrichmentService enrichment, RawArchiveService raw) { }

    private CollectorComponents newCollectorComponents() {
        JsonOutputService json = new JsonOutputService();
        return new CollectorComponents(
                new GtfsApiClient(java.time.Clock.systemUTC(), Duration.ZERO),
                new GtfsRealtimeParser(),
                json,
                new RealtimeStaticEnrichmentService(json),
                new RawArchiveService());
    }

    private List<MulticityCycleEvidence> collect(Path workspaceRoot, CoreCollectionRunEntity run,
                                                   List<DatabaseFeedContext> feeds,
                                                   Map<String, PreparedDatabaseStatic> prepared,
                                                   Instant runStart, int cyclesPerFeed,
                                                   int intervalSeconds,
                                                   Map<String, FeedState> states) throws IOException {
        // 固定轮次验收仍使用绝对时间调度；正式长期采集则由runContinuous逐轮持续生成事件。
        List<Event> events = new ArrayList<>();
        for (int cycle = 1; cycle <= cyclesPerFeed; cycle++) {
            for (DatabaseFeedContext feed : feeds) {
                events.add(new Event(feed, cycle, runStart.plusSeconds(
                        (long) (cycle - 1) * intervalSeconds + feed.staggerOffsetSeconds())));
            }
        }
        events.sort(Comparator.comparing(Event::scheduledAt));
        CollectorComponents components = newCollectorComponents();
        feeds.forEach(f -> states.computeIfAbsent(f.feedId(), ignored -> new FeedState()));
        List<MulticityCycleEvidence> evidence = new ArrayList<>();
        int requestSequence = 0;
        for (Event event : events) {
            requestSequence++;
            evidence.add(collectEvent(workspaceRoot, run, prepared, event, requestSequence, states, components));
        }
        return evidence;
    }

    private MulticityCycleEvidence collectEvent(Path workspaceRoot, CoreCollectionRunEntity run,
                                                  Map<String, PreparedDatabaseStatic> prepared, Event event,
                                                  int requestSequence, Map<String, FeedState> states,
                                                  CollectorComponents components) throws IOException {
        DatabaseFeedContext feed = event.feed();
        FeedState state = states.computeIfAbsent(feed.feedId(), ignored -> new FeedState());
        sleepUntil(event.scheduledAt());
        log.info("开始调用GTFS Realtime接口下载数据，城市={}，feedId={}，schema={}，cycle={}，requestSequence={}，scheduledAt={}",
                feed.cityName(), feed.feedId(), feed.schemaName(), event.cycle(), requestSequence, event.scheduledAt());
        GtfsHttpResult http = components.client().fetch("REALTIME", feed.realtimeUrl());
        // 每次响应独立计算马来西亚日期，保证跨午夜请求写入正确的日期分区。
        String date = GtfsTime.formatBatchDate(http.responseReceivedAt()
                .atZone(GtfsTime.MALAYSIA_ZONE).toLocalDate());
        UUID duplicateRequest = http.responseSha256() == null ? null : state.priorRequestBySha.get(http.responseSha256());
        int sequence = requestSequence;
        JbApiRequestLogEntity request = schemaRouter.withSchema(feed.schemaName(), () -> requestEvidenceService.create(
                feed.feedUid(), run.uid, duplicateRequest, sequence, event.cycle(), event.scheduledAt(), http));
        long drift = Duration.between(event.scheduledAt(), http.requestStartedAt()).toMillis();
        Path rawPath = null; Path parsedPath = null; Path enrichedPath = null; UUID snapshotUid = null;
        SnapshotPersistenceResult persistence = null; int entityCount = 0; int vehicleCount = 0; Throwable failure = null;
        try {
            if (!http.isHttpOk()) {
                schemaRouter.withSchema(feed.schemaName(), () -> requestEvidenceService.markFilesAndParse(request,
                        "SKIPPED", 0, 0, workspaceRoot, null, null, null, false, "HTTP_FAILED", null));
            } else {
                String occurrence = GtfsTime.formatFileTimestamp(http.responseReceivedAt()) + "_cycle" + event.cycle() + "_" + request.uid;
                rawPath = workspaceRoot.resolve("raw_data").resolve(date).resolve(feed.cityFolder())
                        .resolve(feed.filePrefix() + "_gtfs_realtime_raw_" + occurrence + ".pb");
                parsedPath = BusGpsStorageLayout.dailyJsonRoot(workspaceRoot, date).resolve(feed.cityFolder())
                        .resolve(feed.filePrefix() + "_gtfs_realtime_parsed_full_" + occurrence + ".json");
                enrichedPath = BusGpsStorageLayout.dailyJsonRoot(workspaceRoot, date).resolve(feed.cityFolder())
                        .resolve(feed.filePrefix() + "_gtfs_realtime_enriched_full_" + occurrence + ".json");
                // 数据链顺序固定为：RAW落盘 → RAW重读解析 → JSON落盘 → enriched重读 → 数据库事务。
                components.raw().save(rawPath, http.responseBody());
                GtfsRealtime.FeedMessage message = components.parser().parse(Files.readAllBytes(rawPath));
                entityCount = message.getEntityCount();
                components.json().writeJsonString(parsedPath, components.parser().toJson(message));
                GtfsFeedDefinition definition = definition(feed);
                var enriched = components.enrichment().enrich(run.runCode, definition, message,
                        prepared.get(feed.feedId()).feedData(), http);
                snapshotUid = UUID.randomUUID();
                SnapshotQcSummary qc = state.qc.annotate(feed.feedId(), snapshotUid.toString(), enriched.vehicles(),
                        http.ingestTimestampUtc(), prepared.get(feed.feedId()).bounds());
                vehicleCount = enriched.vehicleCount();
                components.json().writeJsonObject(enrichedPath, enriched.root());
                // 数据库只接受磁盘重读的enriched JSON，禁止直接把内存对象写库。
                JsonNode diskRoot = objectMapper.readTree(enrichedPath.toFile());
                log.info("enriched JSON磁盘重新读取完成，feedId={}，schema={}，cycle={}，requestUid={}，vehicles={}，path={}",
                        feed.feedId(), feed.schemaName(), event.cycle(), request.uid,
                        diskRoot.path("vehicles").size(), relative(workspaceRoot, enrichedPath));
                if (diskRoot.path("vehicles").size() != vehicleCount) {
                    throw new IllegalStateException("磁盘重读车辆数与解析结果不一致");
                }
                UUID referencedSnapshot = http.responseSha256() == null ? null : state.priorSnapshotBySha.get(http.responseSha256());
                SnapshotPersistenceContext context = new SnapshotPersistenceContext(feed.feedUid(), run.uid, request.uid,
                        snapshotUid, referencedSnapshot, referencedSnapshot != null, prepared.get(feed.feedId()).references());
                persistence = schemaRouter.withSchema(feed.schemaName(), () -> realtimePersistenceService.persist(diskRoot, context));
                Path finalRawPath = rawPath; Path finalParsedPath = parsedPath; Path finalEnrichedPath = enrichedPath;
                schemaRouter.withSchema(feed.schemaName(), () -> requestEvidenceService.markFilesAndParse(request,
                        "PARSED", message.getEntityCount(), diskRoot.path("vehicles").size(), workspaceRoot,
                        finalRawPath, finalParsedPath, finalEnrichedPath, referencedSnapshot != null, "SUCCESS", null));
                if (http.responseSha256() != null) {
                    state.priorRequestBySha.putIfAbsent(http.responseSha256(), request.uid);
                    state.priorSnapshotBySha.putIfAbsent(http.responseSha256(), snapshotUid);
                }
                log.info("Realtime快照处理完成，城市={}，feedId={}，schema={}，cycle={}，snapshotUid={}，JSON车辆数={}，数据库写入数={}，QC数量={}，重复观测数={}，Static匹配数={}，处理耗时={}ms",
                        feed.cityName(), feed.feedId(), feed.schemaName(), event.cycle(), snapshotUid, vehicleCount,
                        persistence.observationCount(), persistence.qcCount(), qc.duplicateCount(),
                        qc.staticMatchedCount(), persistence.persistenceElapsedMs());
            }
        } catch (Exception ex) {
            failure = ex;
            Path finalRawPath = rawPath; Path finalParsedPath = parsedPath; Path finalEnrichedPath = enrichedPath;
            int finalEntityCount = entityCount; int finalVehicleCount = vehicleCount;
            schemaRouter.withSchema(feed.schemaName(), () -> requestEvidenceService.markFilesAndParse(request,
                    "FAILED", finalEntityCount, finalVehicleCount, workspaceRoot, finalRawPath, finalParsedPath,
                    finalEnrichedPath, duplicateRequest != null, "PROCESSING_FAILED", ex));
            if (persistence == null) {
                schemaRouter.withSchema(feed.schemaName(), () -> requestEvidenceService.markDatabaseFailure(request, ex));
            }
            log.error("单轮采集处理失败，feedId={}，schema={}，cycle={}，requestUid={}，错误信息={}",
                    feed.feedId(), feed.schemaName(), event.cycle(), request.uid, ex.getMessage(), ex);
        }
        Path metadataPath = BusGpsStorageLayout.dailyJsonRoot(workspaceRoot, date).resolve(feed.cityFolder())
                .resolve(feed.filePrefix() + "_gtfs_realtime_metadata_" + GtfsTime.formatFileTimestamp(http.responseReceivedAt())
                        + "_cycle" + event.cycle() + "_" + request.uid + ".json");
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("run_id", run.runCode); metadata.put("run_uid", run.uid); metadata.put("city", feed.cityName());
        metadata.put("schema", feed.schemaName()); metadata.put("feed_id", feed.feedId()); metadata.put("feed_uid", feed.feedUid());
        metadata.put("request_uid", request.uid); metadata.put("snapshot_uid", snapshotUid); metadata.put("cycle", event.cycle());
        metadata.put("scheduled_at", event.scheduledAt().toString()); metadata.put("request_started_at", http.requestStartedAt().toString());
        metadata.put("response_received_at", http.responseReceivedAt().toString()); metadata.put("http_status", http.statusCode());
        metadata.put("response_bytes", http.responseBytes()); metadata.put("response_sha256", http.responseSha256());
        metadata.put("raw_pb_path", relative(workspaceRoot, rawPath)); metadata.put("parsed_json_path", relative(workspaceRoot, parsedPath));
        metadata.put("enriched_json_path", relative(workspaceRoot, enrichedPath)); metadata.put("entity_count", entityCount);
        metadata.put("vehicle_count", vehicleCount); metadata.put("result", persistence != null ? "SUCCESS" : "FAILED");
        components.json().writeJsonObject(metadataPath, metadata);
        return new MulticityCycleEvidence(feed.cityName(), feed.schemaName(), feed.feedId(), feed.feedUid(),
                event.cycle(), requestSequence, request.uid, snapshotUid, event.scheduledAt(), drift, http.statusCode(),
                failure == null ? http.errorClass() : failure.getClass().getSimpleName(), rawPath, parsedPath, enrichedPath,
                http.responseSha256(), entityCount, vehicleCount, persistence);
    }

    private Map<String, Object> databaseBaseline(UUID runUid, List<DatabaseFeedContext> feeds, Map<String, Object> fingerprint) {
        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("core.collection_run", jdbc.queryForObject("SELECT COUNT(*) FROM core.collection_run", Long.class));
        for (String schema : List.of("jb", "kuching", "kl", "melaka")) {
            for (String table : List.of("api_request_log", "realtime_snapshot", "vehicle_observation", "vehicle_observation_qc", "vehicle_latest_state")) {
                counts.put(schema + "." + table, jdbc.queryForObject("SELECT COUNT(*) FROM " + schema + "." + table, Long.class));
            }
        }
        Map<String, Object> staticVersions = new LinkedHashMap<>();
        for (DatabaseFeedContext feed : feeds) {
            staticVersions.put(feed.feedId(), jdbc.queryForList("SELECT uid,static_sha256,routes_count,trips_count,stops_count,stop_times_count,shapes_count,shape_points_count FROM "
                    + feed.schemaName() + ".static_version WHERE feed_uid=? ORDER BY downloaded_at DESC", feed.feedUid()));
        }
        Map<String, Object> relationSizes = new LinkedHashMap<>();
        for (String schema : List.of("jb", "kuching", "kl", "melaka")) {
            for (String table : List.of("api_request_log", "realtime_snapshot", "vehicle_observation", "vehicle_observation_qc", "vehicle_latest_state")) {
                String relation = schema + "." + table;
                relationSizes.put(relation, jdbc.queryForMap("SELECT pg_total_relation_size(CAST(? AS regclass)) total_bytes, pg_relation_size(CAST(? AS regclass)) table_bytes, pg_indexes_size(CAST(? AS regclass)) index_bytes",
                        relation, relation, relation));
            }
        }
        return Map.of("captured_at", Instant.now().toString(), "run_uid", runUid.toString(), "row_counts", counts,
                "static_versions", staticVersions, "database_bytes", jdbc.queryForObject("SELECT pg_database_size(current_database())", Long.class),
                "relation_sizes", relationSizes, "schema_fingerprint", fingerprint);
    }

    private RunProfile validateExplicitConfiguration() {
        if (!properties.multicityDbTest.isEnabled()) throw new IllegalStateException("multicity-db-test.enabled is false; explicit enable is required");
        validateRuntimeEnvironment();
        int duration = properties.multicityDbTest.getDurationMinutes();
        int cycles = properties.multicityDbTest.getCyclesPerFeed();
        if (duration == 10 && cycles == 5) return tenMinuteProfile();
        if (duration == 120 && cycles == 60) return twoHourProfile();
        throw new IllegalStateException("Allowed multicity profiles are 10 minutes/5 cycles or 120 minutes/60 cycles");
    }

    private void validateRuntimeEnvironment() {
        if (datasourcePassword == null || datasourcePassword.isBlank()) {
            throw new IllegalStateException("MISSING_DATASOURCE_PASSWORD_CONFIGURATION");
        }
        int interval = properties.multicityDbTest.getIntervalSeconds();
        if (properties.multicityDbTest.getFeeds().size() != 5 || interval != 120) {
            throw new IllegalStateException("Multicity acceptance requires 5 feeds and 120-second interval");
        }
    }

    private RunProfile tenMinuteProfile() {
        return new RunProfile("MULTICITY_DB_TEN_MINUTE", "multicity_db10m_", "multicity_db_10min.lock",
                5, 120, 25, "Five-feed ten-minute disk JSON to PostgreSQL explicit validation run");
    }

    private RunProfile twoHourProfile() {
        return new RunProfile("MULTICITY_DB_TWO_HOUR", "multicity_db2h_", "multicity_db_2h.lock",
                60, 120, 300, "Five-feed two-hour continuous collection window");
    }

    private void validateFeedMapping(List<DatabaseFeedContext> feeds) {
        Map<String, String> expected = Map.of("mybas-johor", "jb", "mybas-kuching", "kuching", "rapid-bus-kl", "kl",
                "rapid-bus-mrtfeeder", "kl", "mybas-melaka", "melaka");
        if (feeds.size() != 5) throw new IllegalStateException("Five database feeds are required");
        for (DatabaseFeedContext feed : feeds) {
            if (!expected.containsKey(feed.feedId()) || !expected.get(feed.feedId()).equals(feed.schemaName())) {
                throw new IllegalStateException("SCHEMA_MAPPING_CONFLICT: " + feed.feedId() + " -> " + feed.schemaName());
            }
            if (feed.pollIntervalSeconds() != 120) throw new IllegalStateException("POLL_INTERVAL_CONFIG_CONFLICT: " + feed.feedId());
        }
    }

    private GtfsFeedDefinition definition(DatabaseFeedContext feed) {
        return new GtfsFeedDefinition(feed.feedId(), feed.cityCode(), feed.cityFolder(), feed.cityName(), "database",
                "bus", feed.realtimeUrl(), feed.staticUrl(), feed.filePrefix(), true);
    }

    private void sleepUntil(Instant time) {
        while (Instant.now().isBefore(time)) {
            try { Thread.sleep(Math.min(Duration.between(Instant.now(), time).toMillis(), 30_000L)); }
            catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IllegalStateException("绝对调度等待被中断", ex); }
        }
    }

    private void acquireLock(Path lock) {
        try { Files.createDirectories(lock.getParent()); Files.createFile(lock); Files.writeString(lock, "started=" + Instant.now()); }
        catch (FileAlreadyExistsException ex) {
            log.error("多城市采集运行锁冲突，lockName={}", lock.getFileName());
            throw new IllegalStateException("Multicity DB test is already running: " + lock);
        }
        catch (IOException ex) {
            log.error("多城市采集运行锁创建失败，lockName={}，错误信息={}", lock.getFileName(), ex.getMessage());
            throw new IllegalStateException("Cannot create multicity DB test lock", ex);
        }
    }

    private void releaseLock(Path lock) {
        try { Files.deleteIfExists(lock); } catch (IOException ex) { log.error("多城市测试锁释放失败，lock={}", lock, ex); }
    }

    private void writeJson(Path path, Object value) throws IOException {
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), value);
    }

    private String relative(Path root, Path path) {
        return path == null ? null : root.relativize(path).toString().replace('\\', '/');
    }

    private boolean isInterruption(Throwable error) {
        if (Thread.currentThread().isInterrupted()) return true;
        Throwable current = error;
        while (current != null) {
            if (current instanceof InterruptedException) return true;
            current = current.getCause();
        }
        return false;
    }

    /** 无固定时长采集的轻量累计统计；不保存全部历史Evidence，避免长时间运行内存持续增长。 */
    private static final class ContinuousStats {
        private int completedCycles;
        private int actualRequests;
        private int successful;
        private int failed;
        private int http429;
        private int http5xx;
        private int timeouts;
        private long observations;
        private long qcRows;
        private Double driftMaxMs;

        private void accept(MulticityCycleEvidence evidence) {
            actualRequests++;
            if (evidence.persistenceResult() != null) {
                successful++;
                observations += evidence.persistenceResult().observationCount();
                qcRows += evidence.persistenceResult().qcCount();
            } else {
                failed++;
            }
            if (evidence.httpStatus() == 429) http429++;
            if (evidence.httpStatus() >= 500 && evidence.httpStatus() <= 599) http5xx++;
            if ("HttpTimeoutException".equals(evidence.errorClass())) timeouts++;
            double drift = evidence.schedulerDriftMs();
            if (driftMaxMs == null || drift > driftMaxMs) driftMaxMs = drift;
        }
    }

    /** 连续运行期间保留的请求SHA、快照SHA和车辆QC状态。 */
    private static final class FeedState {
        private final JohorRealtimeQcAnnotator qc = new JohorRealtimeQcAnnotator();
        private final Map<String, UUID> priorRequestBySha = new HashMap<>();
        private final Map<String, UUID> priorSnapshotBySha = new HashMap<>();
    }

    /** 十分钟或两小时运行档位的不可变计划参数。 */
    private record RunProfile(String runType, String runCodePrefix, String lockName,
                              int cyclesPerFeed, int intervalSeconds, int expectedAttempts,
                              String remarks) { }
}
