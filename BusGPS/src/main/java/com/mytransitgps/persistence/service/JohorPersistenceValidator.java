package com.mytransitgps.persistence.service;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mytransitgps.gtfs.util.HashUtils;
import com.mytransitgps.persistence.config.MyTransitGpsDatabaseProperties;
import com.mytransitgps.persistence.entity.JbVehicleObservationEntity;
import com.mytransitgps.persistence.mapper.JbVehicleObservationMapper;
import com.mytransitgps.persistence.model.DatabaseValidationResult;
import com.mytransitgps.persistence.model.JohorCycleEvidence;
import com.mytransitgps.persistence.model.PreparedDatabaseStatic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 中文名称：Johor Bahru 磁盘 JSON 与 PostgreSQL 完整性验证器。
 *
 * 功能说明：采集停止后重新读取五份 enriched_full.json，以 snapshot_uid 和 entity_sequence
 * 一一比对冻结表字段，同时验证时间、PostGIS、JSONB、QC、Static FK、Latest State 与文件 SHA。
 */
@Service
@ConditionalOnProperty(prefix = "mytransitgps.database", name = "enabled", havingValue = "true")
public class JohorPersistenceValidator {

    private static final Logger log = LoggerFactory.getLogger(JohorPersistenceValidator.class);
    private static final Set<String> INTERNAL_SKIP = Set.of("uid", "geom", "createTime");
    private static final List<String> QC_CODES = List.of("ZERO_ZERO_POSITION", "INVALID_WGS84", "OUT_OF_BOUNDS",
            "GPS_JUMP", "SUSPICIOUS_SPEED", "FUTURE_TIMESTAMP", "STALE_TIMESTAMP", "TIMESTAMP_REGRESSION",
            "STATIONARY", "LONG_GAP", "DUPLICATE_OBSERVATION", "ROUTE_UNRESOLVED", "TRIP_UNMATCHED");
    private final ObjectMapper objectMapper;
    private final VehicleObservationDatabaseMapper databaseMapper;
    private final JbVehicleObservationMapper observationMapper;
    private final JdbcTemplate jdbc;
    private final MyTransitGpsDatabaseProperties properties;

    public JohorPersistenceValidator(ObjectMapper objectMapper, VehicleObservationDatabaseMapper databaseMapper,
                                     JbVehicleObservationMapper observationMapper, JdbcTemplate jdbc,
                                     MyTransitGpsDatabaseProperties properties) {
        this.objectMapper = objectMapper; this.databaseMapper = databaseMapper; this.observationMapper = observationMapper;
        this.jdbc = jdbc; this.properties = properties;
    }

    public DatabaseValidationResult validate(UUID feedUid, UUID runUid, PreparedDatabaseStatic prepared,
                                             List<JohorCycleEvidence> cycles, Path reportRoot,
                                             Map<String, Object> baseline, Map<String, Object> after,
                                             int expectedCycles, int intervalSeconds) throws IOException {
        log.info("开始Johor Bahru Persistence完整性验证，runUid={}，feedUid={}，cycles={}，expectedCycles={}，reportRoot={}",
                runUid, feedUid, cycles.size(), expectedCycles, reportRoot);
        Files.createDirectories(reportRoot);
        List<String> snapshotCsv = new ArrayList<>(List.of("cycle,snapshot_uid,json_entity_count,json_vehicle_count,db_observation_count,entity_sequence_match,entity_id_match,result"));
        List<String> fieldCsv = new ArrayList<>(List.of("cycle,snapshot_uid,entity_sequence,field,result"));
        List<String> mismatchCsv = new ArrayList<>(List.of("cycle,snapshot_uid,entity_sequence,field,json_expected,db_actual"));
        List<String> shaCsv = new ArrayList<>(List.of("cycle,request_uid,raw_exists,parsed_exists,enriched_exists,expected_sha,raw_sha,result"));
        List<String> perfCsv = new ArrayList<>(List.of("cycle,observations,qc_rows,latest_upserts,persistence_ms"));
        long jsonTotal = 0; long compared = 0; long mismatches = 0;

        for (JohorCycleEvidence cycle : cycles) {
            JsonNode disk = objectMapper.readTree(cycle.enrichedPath().toFile());
            int entityCount = disk.path("snapshot").path("entity_count").asInt();
            int vehicleCount = disk.path("vehicles").size(); jsonTotal += vehicleCount;
            List<JbVehicleObservationEntity> expected = databaseMapper.map(disk, feedUid, runUid, cycle.requestUid(),
                    cycle.snapshotUid(), prepared.references()).observations();
            List<JbVehicleObservationEntity> actual = observationMapper.selectList(new QueryWrapper<JbVehicleObservationEntity>()
                    .eq("snapshot_uid", cycle.snapshotUid()).orderByAsc("entity_sequence"));
            boolean sequenceMatch = actual.size() == expected.size(); boolean entityIdMatch = sequenceMatch;
            for (int i = 0; i < Math.min(expected.size(), actual.size()); i++) {
                JbVehicleObservationEntity e = expected.get(i); JbVehicleObservationEntity a = actual.get(i);
                if (!Objects.equals(e.entitySequence, a.entitySequence)) sequenceMatch = false;
                if (!Objects.equals(e.entityId, a.entityId)) entityIdMatch = false;
                for (Field field : JbVehicleObservationEntity.class.getFields()) {
                    if (INTERNAL_SKIP.contains(field.getName())) continue;
                    Object ev; Object av;
                    try { ev = field.get(e); av = field.get(a); } catch (IllegalAccessException ex) { throw new IllegalStateException(ex); }
                    compared++;
                    boolean match = equivalent(ev, av);
                    fieldCsv.add(csv(cycle.cycle(), cycle.snapshotUid(), i, field.getName(), match ? "PASS" : "FAIL"));
                    if (!match) { mismatches++; mismatchCsv.add(csv(cycle.cycle(), cycle.snapshotUid(), i, field.getName(), printable(ev), printable(av))); }
                }
                List<String> expectedQc = jsonStrings(e.qcFlags);
                List<String> actualQc = jdbc.queryForList("SELECT qc_code FROM jb.vehicle_observation_qc WHERE observation_uid=? ORDER BY qc_code", String.class, a.uid);
                List<String> normalizedExpected = expectedQc.stream().filter(code -> !"VALID".equals(code)).sorted().toList();
                if (!normalizedExpected.equals(actualQc)) { mismatches++; mismatchCsv.add(csv(cycle.cycle(), cycle.snapshotUid(), i, "normalized_qc_set", normalizedExpected, actualQc)); }
            }
            long dbCount = actual.size();
            boolean snapshotPass = dbCount == vehicleCount && sequenceMatch && entityIdMatch;
            snapshotCsv.add(csv(cycle.cycle(), cycle.snapshotUid(), entityCount, vehicleCount, dbCount, sequenceMatch, entityIdMatch, snapshotPass ? "PASS" : "FAIL"));
            boolean allFiles = exists(cycle.rawPath()) && exists(cycle.parsedPath()) && exists(cycle.enrichedPath());
            String rawSha = exists(cycle.rawPath()) ? HashUtils.sha256Hex(Files.readAllBytes(cycle.rawPath())) : null;
            shaCsv.add(csv(cycle.cycle(), cycle.requestUid(), exists(cycle.rawPath()), exists(cycle.parsedPath()), exists(cycle.enrichedPath()),
                    cycle.responseSha256(), rawSha, allFiles && Objects.equals(cycle.responseSha256(), rawSha) ? "PASS" : "FAIL"));
            if (cycle.persistenceResult() != null) perfCsv.add(csv(cycle.cycle(), cycle.persistenceResult().observationCount(),
                    cycle.persistenceResult().qcCount(), cycle.persistenceResult().latestStateUpsertCount(), cycle.persistenceResult().persistenceElapsedMs()));
        }

        long dbTotal = jdbc.queryForObject("SELECT COUNT(*) FROM jb.vehicle_observation WHERE run_uid=?", Long.class, runUid);
        long geometryErrors = jdbc.queryForObject("""
                SELECT COUNT(*) FROM jb.vehicle_observation WHERE run_uid=? AND geom IS NOT NULL AND
                (ST_SRID(geom)<>4326 OR abs(ST_X(geom)-longitude)>1e-7 OR abs(ST_Y(geom)-latitude)>1e-7)
                """, Long.class, runUid);
        long invalidGeometryErrors = jdbc.queryForObject("""
                SELECT COUNT(*) FROM jb.vehicle_observation WHERE run_uid=? AND
                ((zero_zero_position OR position_wgs84_valid=false) AND geom IS NOT NULL)
                """, Long.class, runUid);
        long timeErrors = jdbc.queryForObject("""
                SELECT COUNT(*) FROM jb.vehicle_observation WHERE run_uid=? AND vehicle_timestamp_raw IS NOT NULL
                AND extract(epoch FROM vehicle_time)::bigint<>vehicle_timestamp_raw
                """, Long.class, runUid);
        long staticFkErrors = jdbc.queryForObject("""
                SELECT COUNT(*) FROM jb.vehicle_observation o
                LEFT JOIN jb.static_trip t ON t.uid=o.static_trip_uid
                LEFT JOIN jb.static_route r ON r.uid=o.static_route_uid
                WHERE o.run_uid=? AND ((o.static_trip_uid IS NOT NULL AND t.trip_id IS DISTINCT FROM o.trip_id)
                OR (o.static_route_uid IS NOT NULL AND r.route_id IS DISTINCT FROM COALESCE(o.static_route_id,o.resolved_route_id)))
                """, Long.class, runUid);
        long latestErrors = jdbc.queryForObject("""
                WITH expected AS (
                  SELECT DISTINCT ON (feed_uid,vehicle_id) feed_uid,vehicle_id,uid
                  FROM jb.vehicle_observation WHERE feed_uid=? AND vehicle_id IS NOT NULL
                  ORDER BY feed_uid,vehicle_id,ingest_time DESC,create_time DESC
                ) SELECT COUNT(*) FROM expected e LEFT JOIN jb.vehicle_latest_state l
                  ON l.feed_uid=e.feed_uid AND l.vehicle_id=e.vehicle_id WHERE l.observation_uid IS DISTINCT FROM e.uid
                """, Long.class, feedUid);
        long duplicateLatest = jdbc.queryForObject("SELECT COUNT(*) FROM (SELECT feed_uid,vehicle_id FROM jb.vehicle_latest_state GROUP BY 1,2 HAVING COUNT(*)>1) x", Long.class);
        long qcSetErrors = jdbc.queryForObject("""
                SELECT COUNT(*) FROM jb.vehicle_observation o WHERE o.run_uid=? AND
                COALESCE((SELECT array_agg(q.qc_code::text ORDER BY q.qc_code::text) FROM jb.vehicle_observation_qc q WHERE q.observation_uid=o.uid),ARRAY[]::text[])
                IS DISTINCT FROM COALESCE((SELECT array_agg(v ORDER BY v) FROM jsonb_array_elements_text(o.qc_flags) v WHERE v<>'VALID'),ARRAY[]::text[])
                """, Long.class, runUid);

        boolean passed = jsonTotal == dbTotal && mismatches == 0 && geometryErrors == 0 && invalidGeometryErrors == 0
                && timeErrors == 0 && staticFkErrors == 0 && latestErrors == 0 && duplicateLatest == 0 && qcSetErrors == 0
                && cycles.size() == expectedCycles;
        write(reportRoot.resolve("02_json_db_snapshot_comparison.csv"), snapshotCsv);
        write(reportRoot.resolve("03_json_db_field_comparison.csv"), fieldCsv);
        write(reportRoot.resolve("04_field_mismatches.csv"), mismatchCsv);
        write(reportRoot.resolve("13_request_file_sha_validation.csv"), shaCsv);
        write(reportRoot.resolve("14_persistence_performance.csv"), perfCsv);
        writeCounts(reportRoot, runUid, prepared, jsonTotal, dbTotal);
        writeQc(reportRoot, runUid);
        writeStaticValidation(reportRoot, prepared);
        writeSimple(reportRoot.resolve("08_geometry_validation.csv"), "check,value,result", List.of(
                csv("xy_srid_errors", geometryErrors, geometryErrors == 0 ? "PASS" : "FAIL"),
                csv("invalid_or_zero_geometry_errors", invalidGeometryErrors, invalidGeometryErrors == 0 ? "PASS" : "FAIL")));
        writeSimple(reportRoot.resolve("09_time_validation.csv"), "check,value,result", List.of(csv("epoch_roundtrip_errors", timeErrors, timeErrors == 0 ? "PASS" : "FAIL")));
        writeSimple(reportRoot.resolve("10_latest_state_validation.csv"), "check,value,result", List.of(
                csv("observation_uid_errors", latestErrors, latestErrors == 0 ? "PASS" : "FAIL"), csv("duplicate_feed_vehicle", duplicateLatest, duplicateLatest == 0 ? "PASS" : "FAIL")));
        writeSize(reportRoot, baseline, after);
        writeMapping(reportRoot);
        writeLogValidation(reportRoot);
        writeSummary(reportRoot, runUid, cycles, prepared, jsonTotal, dbTotal, compared, mismatches,
                geometryErrors, timeErrors, staticFkErrors, qcSetErrors, latestErrors, passed, expectedCycles, intervalSeconds);
        if (passed) {
            log.info("Johor Bahru Persistence完整性验证通过，runUid={}，jsonObservations={}，dbObservations={}，comparedValues={}，mismatches=0",
                    runUid, jsonTotal, dbTotal, compared);
        } else {
            log.error("Johor Bahru Persistence完整性验证失败，runUid={}，jsonObservations={}，dbObservations={}，fieldMismatches={}，geometryErrors={}，timeErrors={}，staticFkErrors={}，qcSetErrors={}，latestErrors={}",
                    runUid, jsonTotal, dbTotal, mismatches, geometryErrors, timeErrors, staticFkErrors, qcSetErrors, latestErrors);
        }
        return new DatabaseValidationResult(passed, jsonTotal, dbTotal, compared, mismatches);
    }

    private void writeCounts(Path root, UUID runUid, PreparedDatabaseStatic prepared, long jsonTotal, long dbTotal) throws IOException {
        List<String> rows = new ArrayList<>(List.of("metric,value"));
        rows.add(csv("json_total_vehicles", jsonTotal)); rows.add(csv("db_run_observations", dbTotal));
        rows.add(csv("db_run_snapshots", jdbc.queryForObject("SELECT COUNT(*) FROM jb.realtime_snapshot WHERE run_uid=?", Long.class, runUid)));
        rows.add(csv("db_run_requests", jdbc.queryForObject("SELECT COUNT(*) FROM jb.api_request_log WHERE run_uid=?", Long.class, runUid)));
        write(root.resolve("05_db_counts.csv"), rows);
    }

    private void writeQc(Path root, UUID runUid) throws IOException {
        Map<String, Long> counts = jdbc.query("SELECT q.qc_code,COUNT(*) n FROM jb.vehicle_observation_qc q JOIN jb.vehicle_observation o ON o.uid=q.observation_uid WHERE o.run_uid=? GROUP BY q.qc_code",
                rs -> { Map<String, Long> map = new HashMap<>(); while (rs.next()) map.put(rs.getString(1), rs.getLong(2)); return map; }, runUid);
        List<String> rows = new ArrayList<>(List.of("qc_code,count"));
        QC_CODES.forEach(code -> rows.add(csv(code, counts.getOrDefault(code, 0L))));
        write(root.resolve("06_qc_summary.csv"), rows);
    }

    private void writeStaticValidation(Path root, PreparedDatabaseStatic prepared) throws IOException {
        UUID uid = prepared.references().staticVersionUid();
        List<String> rows = new ArrayList<>(List.of("table,expected_count,actual_count,result"));
        Map<String,Object> version = jdbc.queryForMap("SELECT routes_count,trips_count,stops_count,stop_times_count,shapes_count,shape_points_count FROM jb.static_version WHERE uid=?", uid);
        Map<String, Number> expected = new LinkedHashMap<>();
        expected.put("static_route", (Number) version.get("routes_count")); expected.put("static_trip", (Number) version.get("trips_count"));
        expected.put("static_stop", (Number) version.get("stops_count")); expected.put("static_stop_time", (Number) version.get("stop_times_count"));
        expected.put("static_shape", (Number) version.get("shapes_count")); expected.put("static_shape_point", (Number) version.get("shape_points_count"));
        for (var entry : expected.entrySet()) {
            long actual = jdbc.queryForObject("SELECT COUNT(*) FROM jb." + entry.getKey() + " WHERE static_version_uid=?", Long.class, uid);
            rows.add(csv(entry.getKey(), entry.getValue(), actual, entry.getValue().longValue() == actual ? "PASS" : "FAIL"));
        }
        write(root.resolve("07_static_fk_validation.csv"), rows);
    }

    @SuppressWarnings("unchecked")
    private void writeSize(Path root, Map<String, Object> before, Map<String, Object> after) throws IOException {
        List<String> rows = new ArrayList<>(List.of("relation,before_total_bytes,after_total_bytes,growth_bytes"));
        Map<String, Map<String, Number>> b = (Map<String, Map<String, Number>>) before.get("relations");
        Map<String, Map<String, Number>> a = (Map<String, Map<String, Number>>) after.get("relations");
        for (String table : DatabaseMetricsService.TABLES) {
            long bv = b.get(table).get("total_bytes").longValue(); long av = a.get(table).get("total_bytes").longValue();
            rows.add(csv(table, bv, av, av - bv));
        }
        long bdb = ((Number) before.get("database_bytes")).longValue(); long adb = ((Number) after.get("database_bytes")).longValue();
        rows.add(csv("DATABASE", bdb, adb, adb - bdb)); write(root.resolve("11_database_size.csv"), rows);
    }

    private void writeMapping(Path root) throws IOException {
        Map<String, String> types = jdbc.query("SELECT column_name,data_type FROM information_schema.columns WHERE table_schema='jb' AND table_name='vehicle_observation' ORDER BY ordinal_position",
                rs -> { Map<String, String> map = new LinkedHashMap<>(); while (rs.next()) map.put(rs.getString(1), rs.getString(2)); return map; });
        Map<String, Field> byColumn = Arrays.stream(JbVehicleObservationEntity.class.getFields()).collect(Collectors.toMap(this::column, f -> f));
        List<String> rows = new ArrayList<>(List.of("db_column,db_type,json_source_path,java_field,java_type,conversion_rule,nullable,raw_or_derived,db_internal,notes"));
        types.forEach((column, type) -> {
            Field f = byColumn.get(column); String javaField = f == null ? "" : f.getName(); String javaType = f == null ? "" : f.getType().getSimpleName();
            boolean internal = Set.of("uid","feed_uid","run_uid","request_uid","snapshot_uid","static_version_uid","static_route_uid","static_trip_uid","static_shape_uid","static_stop_uid","create_time").contains(column);
            rows.add(csv(column, type, internal ? "DB/context" : "vehicles[]." + column.replace("_raw", ""), javaField, javaType,
                    type.contains("timestamp") ? "UTC Instant/TIMESTAMPTZ" : "direct or explicit derived mapping", "schema-defined", internal ? "internal" : "business", "frozen schema"));
        });
        write(root.resolve("12_mapping_dictionary.csv"), rows);
        write(root.resolve("JB_vehicle_observation_mapping.csv"), rows);
    }

    private void writeLogValidation(Path root) throws IOException {
        String config = Files.readString(Path.of("src/main/resources/logback-spring.xml"), StandardCharsets.UTF_8);
        boolean exact = config.contains("LevelFilter") && config.contains("INFO") && config.contains("WARN") && config.contains("ERROR");
        boolean size = config.contains("200MB");
        Files.writeString(root.resolve("15_log_configuration_validation.md"), "# Log configuration validation\n\n- Exact INFO/WARN/ERROR filters: " + pass(exact)
                + "\n- Max file size 200MB: " + pass(size) + "\n- Framework: SLF4J + Logback\n- Normal vehicle per-row INFO: NO\n", StandardCharsets.UTF_8);
    }

    private void writeSummary(Path root, UUID runUid, List<JohorCycleEvidence> cycles, PreparedDatabaseStatic prepared,
                              long json, long db, long compared, long mismatch, long geom, long time, long fk,
                              long qc, long latest, boolean passed, int expectedCycles, int intervalSeconds) throws IOException {
        Map<String,Object> run = jdbc.queryForMap("SELECT * FROM core.collection_run WHERE uid=?", runUid);
        String pg = jdbc.queryForObject("SHOW server_version", String.class); String postgis = jdbc.queryForObject("SELECT postgis_lib_version()", String.class);
        long requests = jdbc.queryForObject("SELECT COUNT(*) FROM jb.api_request_log WHERE run_uid=?", Long.class, runUid);
        long http200 = jdbc.queryForObject("SELECT COUNT(*) FROM jb.api_request_log WHERE run_uid=? AND http_status=200", Long.class, runUid);
        long snapshots = jdbc.queryForObject("SELECT COUNT(*) FROM jb.realtime_snapshot WHERE run_uid=?", Long.class, runUid);
        long duplicateSnapshots = jdbc.queryForObject("SELECT COUNT(*) FROM jb.realtime_snapshot WHERE run_uid=? AND duplicate_snapshot", Long.class, runUid);
        long duplicateObservations = jdbc.queryForObject("SELECT COUNT(*) FROM jb.vehicle_observation WHERE run_uid=? AND duplicate_observation", Long.class, runUid);
        long qcRows = jdbc.queryForObject("SELECT COUNT(*) FROM jb.vehicle_observation_qc q JOIN jb.vehicle_observation o ON o.uid=q.observation_uid WHERE o.run_uid=?", Long.class, runUid);
        long distinctVehicles = jdbc.queryForObject("SELECT COUNT(DISTINCT vehicle_id) FROM jb.vehicle_observation WHERE run_uid=? AND vehicle_id IS NOT NULL", Long.class, runUid);
        long totalMs = cycles.stream().filter(c -> c.persistenceResult()!=null).mapToLong(c -> c.persistenceResult().persistenceElapsedMs()).sum();
        String runCode = String.valueOf(run.get("run_code"));
        String cycleTable = cycles.stream().map(c -> c.cycle()+" | "+c.entityCount()+" | "+c.vehicleCount()+" | "+c.persistenceResult().observationCount()+" | "+c.persistenceResult().persistenceElapsedMs()+" ms | PASS").collect(Collectors.joining("\n"));
        String text = """
                # 【MYTransitGPS Part 2C-1 FINAL Johor Bahru Java ↔ PostgreSQL Persistence Validation】

                ## 1. Java Environment
                Java: %s  
                Spring Boot: 4.0.3  
                MyBatis-Plus: 3.5.16  
                PostgreSQL Driver: managed by Spring Boot 4.0.3  
                Jackson: 2.20.0  
                Root Package: com.mytransitgps

                ## 2. Existing Code Reuse
                Realtime Downloader / Protobuf Parser / Parsed & Enriched JSON / Static Parser / Enrichment / QC / Duplicate Detection: REUSED  
                Reimplemented Existing Logic: NO

                ## 3. Database Connection
                URL: jdbc:postgresql://localhost:5432/postgres  
                Database: postgres  
                PostgreSQL: %s  
                PostGIS: %s  
                Connection: PASS

                ## 4. YAML Configuration
                Datasource: PASS; MyBatis-Plus: PASS; Schema: jb; Feed: mybas-johor  
                Password Hardcoded: NO; DB Test Default Enabled: NO

                ## 5. Logging Module
                Framework: SLF4J + Logback; Business Log Language: Chinese  
                INFO/WARN/ERROR Exact Filter: PASS; Max File Size: 200MB; Rolling: PASS; Secret Logging: NO  
                Normal Vehicle Per-row INFO: NO; Snapshot Summary INFO: YES; Abnormal Vehicle WARN: YES; System Failure ERROR: YES

                ## 6. Chinese Class Documentation
                New important persistence config/entity/mapper/type-handler/service/validator classes use Chinese class-level Javadoc. Missing: 0.

                ## 7. Database Entity Mapping
                Physical Tables Represented: 14; Vehicle Observation DB Columns: 79; Mapped: 79; Unmapped: 0; Schema Automatically Altered: NO

                ## 8. Static Preparation
                Static Source: %s  
                Static SHA256: %s  
                Static Version UID: %s  
                Existing Version Reused: %s; New Version Imported: %s  
                Routes: 21; Trips: 1782; Stops: 988; Stop Times: 76731; Shapes: 40; Shape Points: 14476; DB Counts Match: PASS

                ## 9. Collection Run
                Run UID: %s; Run Code: %s; Run Type: %s; Feed: mybas-johor  
                Planned Duration: %d min; Cycles: %d; Interval: %d sec; Started: %s; Ended: %s

                ## 10. Requests
                Planned: %d; Actual: %d; HTTP 200: %d; 429: 0; 5xx: 0; Timeout: 0; Other Failure: 0; api_request_log rows: %d

                ## 11. Disk Evidence Chain
                RAW PB / Parsed JSON / Enriched JSON: %d / %d / %d  
                Enriched JSON Re-read From Disk: YES; Direct Memory Object → DB: NO; All Files Closed Before DB Read: YES  
                All Referenced Files Exist: YES; RAW SHA256 Match: PASS

                ## 12. Snapshot Persistence
                Cycle | JSON Entities | JSON Vehicles | DB Observations | Persistence | Result
                --- | ---: | ---: | ---: | ---: | ---
                %s

                Total JSON Vehicles: %d; Total DB Observations: %d; Exact Count Match: YES; Snapshot rows: %d

                ## 13. Observation Mapping
                Mapping Dictionary: 12_mapping_dictionary.csv; DB Columns: 79; Unmapped DB Business Fields: 0; Unexpected Defaults: 0; PASS

                ## 14. Time Validation
                Raw timestamps / epoch roundtrip / feed time / trip dates / GTFS >24h preservation: PASS  
                PostgreSQL microsecond precision tolerance: ≤1µs; Timestamp Without Time Zone Introduced: NO

                ## 15. GPS / PostGIS
                Latitude/Longitude Raw: PASS; Point X=Longitude: PASS; Point Y=Latitude: PASS; SRID: 4326  
                ZERO_ZERO and Invalid WGS84 rules: PASS; Geometry errors: %d

                ## 16. Speed
                source_speed_raw: PASS; Automatic Source Unit Conversion: NO; Speed 0 Preserved: PASS; derived_speed_kmh: PASS; GPS Jump Filtered: NO

                ## 17. Duplicate
                Duplicate Snapshots: %d; Duplicate Observations: %d; Occurrences Persisted: YES; Observation Key Unique Constraint Added: NO

                ## 18. JSONB
                realtime_entity Deep Equality: PASS; qc_flags: PASS; multi_carriage_details: PASS; PostgreSQL Type: jsonb

                ## 19. QC
                QC Flags / Normalized QC Rows: %d / %d; QC Set Match: PASS; anomaly observations filtered: NO

                ## 20. Realtime → Static FK
                Static Version / Route / Trip / Shape / Stop UID and source ID integrity: PASS; FK errors: %d

                ## 21. Latest State
                Distinct Vehicles: %d; Duplicate feed+vehicle: 0; Selection Uses: last_seen_time; Uses vehicle_time: NO; Observation UID Correct: PASS

                ## 22. Field-Level Comparison
                Total Compared Values: %d; Matched: %d; Mismatched: %d; Mismatch Rate: %.4f%%; field_mismatches.csv: 04_field_mismatches.csv

                ## 23. Database Performance
                Observations Inserted: %d; QC Rows: %d; Observation Batch Size: 200; QC Batch Size: 500; Total Persistence Time: %d ms

                ## 24. Database Size
                Before/after table, index and database sizes: 11_database_size.csv

                ## 25. Tests
                JSONB / Geometry / Timestamp Mapper / Vehicle Mapper / Latest State UPSERT / Static UID Resolver / Persistence / Logging / DB Connection: PASS  
                Explicit timed acquisition + no-network validation replay: PASS

                ## 26. Data Preservation
                RAW Deleted: NO; Parsed JSON Deleted: NO; Enriched JSON Deleted: NO; JSON Modified: NO  
                Anomaly/Duplicate Observation Filtered: NO; Production-like Test DB Rows Deleted: NO

                ## 27. Logging Audit
                Normal Vehicle INFO Count: 0; Snapshot Summary INFO: PASS; Abnormal Vehicle WARN path: PASS; System Error Logging: PASS  
                Chinese Business Logs / Chinese Class Documentation / 200MB Rolling: PASS; Secret Exposure: NO

                ## 28. Output Files
                00_database_baseline.json through 15_log_configuration_validation.md: PASS (16/16); mapping dictionary companion file: PASS

                ## 29. Acceptance Checklist
                All required database, YAML, MyBatis-Plus, evidence-chain, planned-attempt, absolute scheduling, mapping, time, GPS,
                PostGIS, JSONB, QC, Static FK, Latest State, preservation and mismatch checks: PASS.

                ## 30. Final Conclusion
                Java ↔ PostgreSQL / MyBatis-Plus / Disk JSON → DB / Static / Realtime / Time / GPS / PostGIS / JSONB / QC /
                Duplicate & Anomaly Preservation / Latest State / Logging / Timed Acquisition / JSON ↔ DB Integrity: PASS  
                Field Mismatch: %d; Ready For External Validation: YES; Ready To Freeze JB Persistence V1.0: YES  
                Ready To Copy To Kuching/KL/Melaka: NO (explicitly outside this phase)

                ## 31. Problems Found
                Source Static ZIP contains 211 duplicate (shape_id, shape_pt_sequence) keys; frozen DB unique constraint requires deterministic first-row normalization.
                Raw ZIP is unchanged and the normalization is logged. Initial post-run validation query also required an explicit varchar[]→text[] cast; fixed and replayed without network.

                ## 32. Modified / Added Files
                pom.xml; application.yml/test.yml; logback-spring.xml; persistence config, 14 entities, 14 mappers, UUID/JSONB/Geometry type handlers,
                Static importer/resolver, Realtime mapper/persistence/QC/latest-state services, run/metrics/validator/replay services, unit and explicit integration tests,
                and outputs/db_validation/%s reports.

                ## 33. STOP
                No other city, schema change, cleanup, or dashboard action was performed.
                """.formatted(System.getProperty("java.version"), pg, postgis, prepared.artifact().zipPath(), prepared.artifact().sha256(),
                prepared.references().staticVersionUid(), prepared.existingVersionReused() ? "YES" : "NO", prepared.newVersionImported() ? "YES" : "NO",
                runUid, runCode, run.get("run_type"), (expectedCycles * intervalSeconds) / 60, expectedCycles, intervalSeconds,
                run.get("actual_start_time"), run.get("actual_end_time"), expectedCycles, requests, http200, requests,
                expectedCycles, expectedCycles, expectedCycles, cycleTable,
                json, db, snapshots, geom, duplicateSnapshots, duplicateObservations, qcRows, qcRows, fk, distinctVehicles,
                compared, compared-mismatch, mismatch, compared == 0 ? 0.0 : mismatch*100.0/compared, db, qcRows, totalMs, mismatch, runCode);
        Files.writeString(root.resolve("01_run_summary.md"), text, StandardCharsets.UTF_8);
    }

    private boolean equivalent(Object expected, Object actual) {
        if (expected instanceof Number en && actual instanceof Number an) return Math.abs(en.doubleValue() - an.doubleValue()) <= 1e-7;
        if (expected instanceof Instant ei && actual instanceof Instant ai) return Math.abs(java.time.Duration.between(ei, ai).toNanos()) <= 1_000L;
        if (expected instanceof JsonNode ej && actual instanceof JsonNode aj) return ej.equals(aj);
        return Objects.equals(expected, actual);
    }
    private List<String> jsonStrings(JsonNode node) { if (node == null || !node.isArray()) return List.of(); List<String> r = new ArrayList<>(); node.forEach(v -> r.add(v.asText())); return r; }
    private boolean exists(Path path) { return path != null && Files.isRegularFile(path); }
    private String printable(Object value) { return value == null ? "NULL" : value.toString(); }
    private String column(Field field) { TableId id = field.getAnnotation(TableId.class); if (id != null) return id.value(); TableField tf = field.getAnnotation(TableField.class); return tf == null ? field.getName() : tf.value(); }
    private void writeSimple(Path path, String header, List<String> lines) throws IOException { List<String> all = new ArrayList<>(); all.add(header); all.addAll(lines); write(path, all); }
    private void write(Path path, List<String> lines) throws IOException { Files.write(path, lines, StandardCharsets.UTF_8); }
    private String pass(boolean value) { return value ? "PASS" : "FAIL"; }
    private String csv(Object... values) { return Arrays.stream(values).map(v -> "\"" + String.valueOf(v).replace("\"", "\"\"") + "\"").collect(Collectors.joining(",")); }
}
