package com.mytransitgps.persistence.service;

import com.mytransitgps.common.util.TrafficLogPathResolver;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mytransitgps.persistence.entity.CoreCollectionRunEntity;
import com.mytransitgps.persistence.entity.CoreGtfsFeedEntity;
import com.mytransitgps.persistence.entity.JbApiRequestLogEntity;
import com.mytransitgps.persistence.entity.JbRealtimeSnapshotEntity;
import com.mytransitgps.persistence.mapper.CoreCollectionRunMapper;
import com.mytransitgps.persistence.mapper.JbApiRequestLogMapper;
import com.mytransitgps.persistence.mapper.JbRealtimeSnapshotMapper;
import com.mytransitgps.persistence.model.DatabaseValidationResult;
import com.mytransitgps.persistence.model.JohorCycleEvidence;
import com.mytransitgps.persistence.model.LocalStaticArtifact;
import com.mytransitgps.persistence.model.PreparedDatabaseStatic;
import com.mytransitgps.persistence.model.SnapshotPersistenceResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 中文名称：Johor Bahru 采集后只读验证重放服务。
 *
 * 功能说明：在计划轮次网络请求已经停止后，从 api_request_log 和本地文件重建证据清单，
 * 仅重新读取磁盘 JSON 与 PostgreSQL 完成验证报告；绝不访问 GTFS API。
 */
@Service
@ConditionalOnProperty(prefix = "mytransitgps.database", name = "enabled", havingValue = "true")
public class JohorValidationReplayService {
    private static final Logger log = LoggerFactory.getLogger(JohorValidationReplayService.class);
    private final ObjectMapper objectMapper; private final DatabaseFeedService feedService;
    private final JbStaticPersistenceService staticPersistence; private final CoreCollectionRunMapper runMapper;
    private final JbApiRequestLogMapper requestMapper; private final JbRealtimeSnapshotMapper snapshotMapper;
    private final JdbcTemplate jdbc; private final DatabaseMetricsService metrics; private final JohorPersistenceValidator validator;
    private final CollectionRunService runService;

    public JohorValidationReplayService(ObjectMapper objectMapper, DatabaseFeedService feedService,
                                        JbStaticPersistenceService staticPersistence, CoreCollectionRunMapper runMapper,
                                        JbApiRequestLogMapper requestMapper, JbRealtimeSnapshotMapper snapshotMapper,
                                        JdbcTemplate jdbc, DatabaseMetricsService metrics, JohorPersistenceValidator validator,
                                        CollectionRunService runService) {
        this.objectMapper=objectMapper; this.feedService=feedService; this.staticPersistence=staticPersistence; this.runMapper=runMapper;
        this.requestMapper=requestMapper; this.snapshotMapper=snapshotMapper; this.jdbc=jdbc; this.metrics=metrics;
        this.validator=validator; this.runService=runService;
    }

    public DatabaseValidationResult replay(Path workspaceRoot, String runCode) {
        log.info("开始Johor Bahru只读验证重放，runCode={}，workspaceRoot={}", runCode, workspaceRoot);
        try {
            CoreCollectionRunEntity run = runMapper.selectOne(new QueryWrapper<CoreCollectionRunEntity>().eq("run_code", runCode));
            if (run == null) throw new IllegalStateException("Run not found: " + runCode);
            CoreGtfsFeedEntity feed = feedService.requireFeed("mybas-johor");
            LocalStaticArtifact artifact = new LocalStaticArtifactResolver(objectMapper).resolve(workspaceRoot, "mybas-johor");
            PreparedDatabaseStatic prepared = staticPersistence.prepare(workspaceRoot, feed.uid, "mybas-johor", artifact);
            List<JbApiRequestLogEntity> requests = requestMapper.selectList(new QueryWrapper<JbApiRequestLogEntity>().eq("run_uid", run.uid).orderByAsc("cycle_number"));
            if (!requests.isEmpty() && requests.get(requests.size() - 1).responseReceivedAt != null) {
                run.actualEndTime = requests.get(requests.size() - 1).responseReceivedAt;
                runMapper.updateById(run);
            }
            List<JohorCycleEvidence> cycles = new ArrayList<>();
            for (JbApiRequestLogEntity request : requests) {
                JbRealtimeSnapshotEntity snapshot = snapshotMapper.selectOne(new QueryWrapper<JbRealtimeSnapshotEntity>().eq("request_uid", request.uid));
                if (snapshot == null) throw new IllegalStateException("Snapshot missing for request " + request.uid);
                int observations = jdbc.queryForObject("SELECT COUNT(*) FROM jb.vehicle_observation WHERE snapshot_uid=?", Integer.class, snapshot.uid);
                int qc = jdbc.queryForObject("SELECT COUNT(*) FROM jb.vehicle_observation_qc q JOIN jb.vehicle_observation o ON o.uid=q.observation_uid WHERE o.snapshot_uid=?", Integer.class, snapshot.uid);
                int latest = jdbc.queryForObject("SELECT COUNT(*) FROM jb.vehicle_latest_state l JOIN jb.vehicle_observation o ON o.uid=l.observation_uid WHERE o.snapshot_uid=?", Integer.class, snapshot.uid);
                long elapsed = persistenceElapsed(workspaceRoot, snapshot.uid);
                cycles.add(new JohorCycleEvidence(request.cycleNumber, request.uid, snapshot.uid, request.scheduledAt,
                        request.schedulerDriftMs, request.httpStatus, path(workspaceRoot, request.rawObjectPath),
                        path(workspaceRoot, request.parsedJsonPath), path(workspaceRoot, request.enrichedJsonPath),
                        request.responseSha256, request.entityCount, request.vehicleCount,
                        new SnapshotPersistenceResult(snapshot.uid, observations, qc, observations, elapsed)));
            }
            Path reportRoot = workspaceRoot.resolve("outputs/db_validation").resolve(runCode);
            Map<String,Object> baseline = objectMapper.readValue(reportRoot.resolve("00_database_baseline.json").toFile(), new TypeReference<>() {});
            int expectedCycles = run.plannedCycleCount == null ? cycles.size() : run.plannedCycleCount;
            DatabaseValidationResult result = validator.validate(feed.uid, run.uid, prepared, cycles, reportRoot, baseline, metrics.snapshot(), expectedCycles, 120);
            if (result.passed()) runService.markValidationPassed(run, jdbc.queryForObject("SELECT COUNT(*)>0 FROM jb.vehicle_observation_qc q JOIN jb.vehicle_observation o ON o.uid=q.observation_uid WHERE o.run_uid=?", Boolean.class, run.uid));
            else runService.markValidationFailed(run, "validation replay mismatches=" + result.fieldMismatches());
            if (result.passed()) {
                log.info("Johor Bahru只读验证重放完成，runCode={}，runUid={}，cycles={}，mismatches=0，result=PASS",
                        runCode, run.uid, cycles.size());
            } else {
                log.error("Johor Bahru只读验证重放未通过，runCode={}，runUid={}，cycles={}，mismatches={}",
                        runCode, run.uid, cycles.size(), result.fieldMismatches());
            }
            return result;
        } catch (Exception ex) {
            log.error("Johor Bahru只读验证重放失败，runCode={}，错误信息={}", runCode, ex.getMessage(), ex);
            throw ex instanceof RuntimeException runtime ? runtime : new IllegalStateException(ex);
        }
    }
    private Path path(Path root, String relative) { return relative == null ? null : root.resolve(relative.replace('/', java.io.File.separatorChar)); }
    private long persistenceElapsed(Path workspaceRoot, UUID snapshotUid) {
        Path log = TrafficLogPathResolver.infoLog(workspaceRoot);
        try {
            String marker = "snapshotUid=" + snapshotUid;
            for (String line : java.nio.file.Files.readAllLines(log)) {
                if (line.contains(marker) && line.contains("persistenceMs=")) return Long.parseLong(line.substring(line.lastIndexOf("persistenceMs=") + 14).trim());
            }
        } catch (Exception ignored) { }
        return 0L;
    }
}
