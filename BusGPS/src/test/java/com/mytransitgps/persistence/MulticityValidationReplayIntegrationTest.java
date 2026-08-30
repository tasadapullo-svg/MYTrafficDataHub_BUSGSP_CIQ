package com.mytransitgps.persistence;

import com.mytransitgps.common.util.TrafficLogPathResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mytransitgps.persistence.config.MyTransitGpsDatabaseProperties;
import com.mytransitgps.persistence.entity.CoreCollectionRunEntity;
import com.mytransitgps.persistence.mapper.CoreCollectionRunMapper;
import com.mytransitgps.persistence.model.MulticityCycleEvidence;
import com.mytransitgps.persistence.model.PreparedDatabaseStatic;
import com.mytransitgps.persistence.model.SnapshotPersistenceResult;
import com.mytransitgps.persistence.routing.CitySchemaRouter;
import com.mytransitgps.persistence.service.CollectionRunService;
import com.mytransitgps.persistence.service.DatabaseFeedService;
import com.mytransitgps.persistence.service.JbStaticPersistenceService;
import com.mytransitgps.persistence.service.LocalStaticArtifactResolver;
import com.mytransitgps.persistence.service.MulticityPersistenceValidator;
import com.mytransitgps.persistence.service.SchemaFingerprintService;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 对已完成 Run 进行纯离线验证重放；只读既有 RAW/JSON/PostgreSQL，不发起任何 GTFS 网络请求。
 */
@Tag("multicity-db-validation-replay")
@SpringBootTest(properties = {"mytransitgps.database.enabled=true", "app.redis.connect-on-startup=false"})
class MulticityValidationReplayIntegrationTest {
    @Autowired ObjectMapper objectMapper;
    @Autowired MyTransitGpsDatabaseProperties properties;
    @Autowired DatabaseFeedService feedService;
    @Autowired JbStaticPersistenceService staticService;
    @Autowired CitySchemaRouter router;
    @Autowired MulticityPersistenceValidator validator;
    @Autowired SchemaFingerprintService fingerprintService;
    @Autowired CoreCollectionRunMapper runMapper;
    @Autowired CollectionRunService runService;
    @Autowired JdbcTemplate jdbc;

    @Test void replaysCompletedRunWithoutNetwork() throws Exception {
        assumeTrue(Boolean.getBoolean("mytransitgps.multicity-validation-replay.confirm"), "需要显式确认离线验证重放");
        String runCode = System.getProperty("mytransitgps.multicity-validation-replay.run-code");
        assertThat(runCode).isNotBlank();
        Path workspace = Path.of(System.getProperty("user.dir")).toAbsolutePath().getParent();
        Path reportRoot = workspace.resolve("outputs/db_validation").resolve(runCode);
        CoreCollectionRunEntity run = runMapper.selectOne(new QueryWrapper<CoreCollectionRunEntity>().eq("run_code", runCode));
        assertThat(run).isNotNull();
        var feeds = feedService.requireContexts(properties.multicityDbTest.getFeeds());
        Map<UUID, Long> persistenceMs = persistenceDurations(TrafficLogPathResolver.infoLog(workspace));
        var local = new LocalStaticArtifactResolver(objectMapper);
        Map<String, PreparedDatabaseStatic> prepared = new LinkedHashMap<>();
        for (var feed : feeds) {
            var artifact = local.resolve(workspace, feed.feedId());
            prepared.put(feed.feedId(), router.withSchema(feed.schemaName(),
                    () -> staticService.prepare(workspace, feed.feedUid(), feed.feedId(), artifact)));
        }
        List<MulticityCycleEvidence> attempts = new ArrayList<>();
        for (var feed : feeds) {
            String sql = "SELECT r.*,s.uid snapshot_uid FROM " + feed.schemaName() + ".api_request_log r LEFT JOIN "
                    + feed.schemaName() + ".realtime_snapshot s ON s.request_uid=r.uid WHERE r.run_uid=? AND r.feed_uid=? ORDER BY r.cycle_number";
            for (Map<String, Object> row : jdbc.queryForList(sql, run.uid, feed.feedUid())) {
                UUID snapshot = (UUID) row.get("snapshot_uid");
                int observations = snapshot == null ? 0 : count("SELECT COUNT(*) FROM " + feed.schemaName() + ".vehicle_observation WHERE snapshot_uid=?", snapshot);
                int qc = snapshot == null ? 0 : count("SELECT COUNT(*) FROM " + feed.schemaName() + ".vehicle_observation_qc q JOIN "
                        + feed.schemaName() + ".vehicle_observation o ON o.uid=q.observation_uid WHERE o.snapshot_uid=?", snapshot);
                SnapshotPersistenceResult persistence = snapshot == null || !"SUCCESS".equals(row.get("result")) ? null
                        : new SnapshotPersistenceResult(snapshot, observations, qc, observations, persistenceMs.getOrDefault(snapshot, 0L));
                attempts.add(new MulticityCycleEvidence(feed.cityName(), feed.schemaName(), feed.feedId(), feed.feedUid(),
                        number(row,"cycle_number"), number(row,"request_sequence"), (UUID) row.get("uid"), snapshot,
                        instant(row.get("scheduled_at")), ((Number) row.get("scheduler_drift_ms")).longValue(),
                        number(row,"http_status"), (String) row.get("error_class"), resolve(workspace,row.get("raw_object_path")),
                        resolve(workspace,row.get("parsed_json_path")), resolve(workspace,row.get("enriched_json_path")),
                        (String) row.get("response_sha256"), number(row,"entity_count"), number(row,"vehicle_count"), persistence));
            }
        }
        attempts.sort(java.util.Comparator.comparingInt(MulticityCycleEvidence::requestSequence));
        JsonNode baseline = objectMapper.readTree(reportRoot.resolve("01_database_baseline.json").toFile()).path("schema_fingerprint");
        Map<String, Object> before = Map.of("sha256", baseline.path("sha256").asText(),
                "metadata_row_count", baseline.path("metadata_row_count").asInt(), "table_count", 52);
        var afterFingerprint = fingerprintService.capture();
        var result = validator.validate(run.uid, runCode, feeds, Map.copyOf(prepared), attempts, reportRoot,
                before, afterFingerprint);
        if (result.passed()) {
            runService.markValidationPassed(run, true);
            result = validator.validate(run.uid, runCode, feeds, Map.copyOf(prepared), attempts, reportRoot,
                    before, afterFingerprint);
        }
        assertThat(attempts).hasSize(run.plannedRequestCount);
        assertThat(result.fieldMismatches()).isZero();
        assertThat(result.passed()).isTrue();
    }

    private int count(String sql, Object value) { return jdbc.queryForObject(sql, Integer.class, value); }
    private int number(Map<String,Object> row,String key){Object v=row.get(key);return v==null?0:((Number)v).intValue();}
    private Path resolve(Path root,Object value){return value==null?null:root.resolve(value.toString()).normalize();}
    private Instant instant(Object value){if(value instanceof Timestamp t)return t.toInstant();if(value instanceof OffsetDateTime o)return o.toInstant();return Instant.parse(value.toString());}
    private Map<UUID,Long> persistenceDurations(Path logPath) throws Exception {
        Map<UUID,Long> result=new java.util.HashMap<>();
        var pattern=java.util.regex.Pattern.compile("snapshotUid=([0-9a-f-]{36}).*elapsedMs=(\\d+)");
        for(String line:java.nio.file.Files.readAllLines(logPath,java.nio.charset.StandardCharsets.UTF_8)){
            var matcher=pattern.matcher(line);if(matcher.find())result.put(UUID.fromString(matcher.group(1)),Long.parseLong(matcher.group(2)));
        }
        return result;
    }
}
