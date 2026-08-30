package com.mytransitgps.modules.ciq.persistence;

import com.mytransitgps.modules.ciq.config.CiqProperties;
import com.mytransitgps.modules.ciq.domain.TrafficSpeedBandRecord;
import com.mytransitgps.modules.ciq.domain.TrafficSpeedScopeMatch;
import com.mytransitgps.modules.ciq.quality.TrafficSpeedValidationResult;
import com.mytransitgps.modules.ciq.storage.CiqRawJsonArtifact;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * API01 TrafficSpeedBands PostgreSQL 批量持久化服务。
 *
 * <p>以页内 batch 为单位执行 PostGIS 空间筛选、traffic_link upsert、traffic_link_scope
 * upsert 和 observation insert，避免长期运行时对约 14 万条快照记录逐行访问数据库。</p>
 */
@Component
@ConditionalOnProperty(prefix = "traffic.ciq", name = "enabled", havingValue = "true")
@ConditionalOnProperty(prefix = "traffic.ciq.persistence", name = "database-write-enabled", havingValue = "true")
public class CiqTrafficSpeedPersistenceService {
    private static final Logger log = LoggerFactory.getLogger(CiqTrafficSpeedPersistenceService.class);

    private final JdbcTemplate jdbcTemplate;
    private final CiqProperties properties;

    public CiqTrafficSpeedPersistenceService(JdbcTemplate jdbcTemplate, CiqProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    /**
     * 检查 Woodlands/Tuas 六个明确 study_area 组合是否唯一、启用、有效且 SRID=4326。
     */
    public boolean hasRequiredStudyAreas() {
        Integer missingOrInvalid = jdbcTemplate.queryForObject("""
                WITH required(ciq_code, zone_code) AS (
                    VALUES
                        ('WOODLANDS','CORE'),
                        ('WOODLANDS','APPROACH'),
                        ('WOODLANDS','CORRIDOR'),
                        ('TUAS','CORE'),
                        ('TUAS','APPROACH'),
                        ('TUAS','CORRIDOR')
                ),
                active_area AS (
                    SELECT ciq_code, zone_code, COUNT(*) AS active_count,
                           BOOL_AND(geom IS NOT NULL AND ST_SRID(geom) = 4326 AND ST_IsValid(geom)) AS geom_ok
                    FROM lta.study_area
                    WHERE active = TRUE
                    GROUP BY ciq_code, zone_code
                )
                SELECT COUNT(*)
                FROM required r
                LEFT JOIN active_area a
                  ON a.ciq_code = r.ciq_code
                 AND a.zone_code = r.zone_code
                WHERE COALESCE(a.active_count, 0) <> 1
                   OR COALESCE(a.geom_ok, FALSE) = FALSE
                """, Integer.class);
        return missingOrInvalid != null && missingOrInvalid == 0;
    }

    public UUID apiEndpointUid(String apiCode) {
        return jdbcTemplate.queryForObject(
                "SELECT uid FROM lta.api_endpoint WHERE api_code = ? AND enabled = TRUE",
                UUID.class, apiCode);
    }

    public void beginRun(UUID runUid, UUID apiEndpointUid, String runMode, Instant scheduledTime, Instant startTime) {
        jdbcTemplate.update("""
                INSERT INTO lta.collection_run
                    (uid, api_endpoint_uid, run_mode, scheduled_time, request_start_time, success, snapshot_complete)
                VALUES (?, ?, ?, ?, ?, FALSE, FALSE)
                """, runUid, apiEndpointUid, runMode, ts(scheduledTime), ts(startTime));
    }

    public void logPage(UUID runUid, int pageNo, int skipValue, Instant requestStart, Instant requestEnd,
                        int httpStatus, int recordCount, long responseBytes, boolean success, String errorMessage) {
        jdbcTemplate.update("""
                INSERT INTO lta.collection_page_log
                    (run_uid, page_no, skip_value, request_start_time, request_end_time, http_status,
                     record_count, response_bytes, success, error_message)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (run_uid, page_no) DO UPDATE SET
                    request_end_time = EXCLUDED.request_end_time,
                    http_status = EXCLUDED.http_status,
                    record_count = EXCLUDED.record_count,
                    response_bytes = EXCLUDED.response_bytes,
                    success = EXCLUDED.success,
                    error_message = EXCLUDED.error_message
                """, runUid, pageNo, skipValue, ts(requestStart), ts(requestEnd), httpStatus,
                recordCount, responseBytes, success, errorMessage);
    }

    public void registerArtifact(UUID runUid, CiqRawJsonArtifact artifact) {
        jdbcTemplate.update("""
                INSERT INTO lta.collection_artifact
                    (run_uid, artifact_type, file_name, file_path, file_size_bytes, sha256, compression_type)
                VALUES (?, 'RAW_JSON', ?, ?, ?, ?, 'NONE')
                """, runUid, artifact.path().getFileName().toString(), artifact.path().toString(),
                artifact.sizeBytes(), artifact.sha256());
    }

    @Transactional
    public TrafficSpeedPersistenceResult persistPage(UUID runUid, Instant snapshotTime, TrafficSpeedPageData pageData) {
        Instant started = Instant.now();
        int batchSize = Math.max(1, properties.getPersistence().getBatchSize());
        List<TrafficSpeedValidationResult> accepted = pageData.validationResults().stream()
                .filter(TrafficSpeedValidationResult::acceptedForSpatialFilter)
                .toList();
        TrafficSpeedPersistenceResult total = TrafficSpeedPersistenceResult.empty();
        for (int from = 0; from < accepted.size(); from += batchSize) {
            List<TrafficSpeedValidationResult> batch = accepted.subList(from, Math.min(from + batchSize, accepted.size()));
            total = total.plus(persistBatch(runUid, snapshotTime, pageData.pageNo(), batch));
        }
        log.info("CIQ批量写入完成，runUid={}，page={}，linksInserted={}，linksUpdated={}，scopes={}，observations={}，duplicates={}，durationMs={}",
                runUid, pageData.pageNo(), total.newLinkCount(), total.updatedLinkCount(), total.scopeRows(),
                total.observationsInserted(), total.observationDuplicates(), Duration.between(started, Instant.now()).toMillis());
        return total;
    }

    public void finalizeRun(UUID runUid, Instant endTime, int httpStatus, int pageCount, long recordCount,
                            long responseBytes, boolean success, boolean snapshotComplete, short retryCount,
                            String errorMessage) {
        jdbcTemplate.update("""
                UPDATE lta.collection_run
                SET request_end_time = ?, http_status = ?, page_count = ?, record_count = ?,
                    response_bytes = ?, success = ?, snapshot_complete = ?, retry_count = ?,
                    consistency_status = ?, error_message = ?
                WHERE uid = ?
                """, ts(endTime), httpStatus, pageCount, recordCount, responseBytes, success,
                snapshotComplete, retryCount, success ? "COMPLETE" : "FAILED", errorMessage, runUid);
    }

    private TrafficSpeedPersistenceResult persistBatch(UUID runUid, Instant snapshotTime, int pageNo,
                                                       List<TrafficSpeedValidationResult> batch) {
        if (batch.isEmpty()) return TrafficSpeedPersistenceResult.empty();
        Instant spatialStarted = Instant.now();
        Map<String, List<TrafficSpeedScopeMatch>> matchesByLink = batchScopeMatches(batch);
        long areaMatches = matchesByLink.values().stream().mapToLong(List::size).sum();
        long inScope = matchesByLink.size();
        long outOfScope = batch.stream().map(v -> v.record().linkId()).distinct()
                .filter(linkId -> !matchesByLink.containsKey(linkId)).count();
        log.info("CIQ空间批量筛选完成，runUid={}，page={}，candidate={}，inScope={}，areaMatches={}，durationMs={}",
                runUid, pageNo, batch.size(), inScope, areaMatches,
                Duration.between(spatialStarted, Instant.now()).toMillis());

        List<TrafficSpeedBandRecord> inScopeRecords = batch.stream()
                .map(TrafficSpeedValidationResult::record)
                .filter(record -> matchesByLink.containsKey(record.linkId()))
                .collect(Collectors.toMap(TrafficSpeedBandRecord::linkId, Function.identity(), (a, b) -> a, LinkedHashMap::new))
                .values().stream().toList();
        if (inScopeRecords.isEmpty()) {
            return new TrafficSpeedPersistenceResult(0, 0, 0, 0, 0, outOfScope, 0);
        }

        Map<String, LinkState> before = existingLinks(inScopeRecords);
        batchUpsertLinks(inScopeRecords, snapshotTime);
        Map<String, UUID> linkUids = fetchLinkUids(inScopeRecords);
        long insertedLinks = inScopeRecords.stream().filter(record -> !before.containsKey(record.linkId())).count();
        long updatedLinks = inScopeRecords.stream().filter(record -> {
            LinkState state = before.get(record.linkId());
            return state != null && state.changedFrom(record);
        }).count();

        int scopeRows = batchUpsertScopes(linkUids, matchesByLink);
        int observations = batchInsertObservations(runUid, snapshotTime, inScopeRecords, linkUids);
        long duplicates = inScopeRecords.size() - observations;
        return new TrafficSpeedPersistenceResult(insertedLinks, updatedLinks, scopeRows, observations,
                duplicates, outOfScope, inScopeRecords.size());
    }

    private Map<String, List<TrafficSpeedScopeMatch>> batchScopeMatches(List<TrafficSpeedValidationResult> batch) {
        String values = batch.stream().map(ignored -> "(?, ?, ?, ?, ?)").collect(Collectors.joining(","));
        List<Object> args = new ArrayList<>(batch.size() * 5);
        for (TrafficSpeedValidationResult item : batch) {
            TrafficSpeedBandRecord r = item.record();
            args.add(r.linkId());
            args.add(r.startLon());
            args.add(r.startLat());
            args.add(r.endLon());
            args.add(r.endLat());
        }
        String sql = """
                WITH candidate(link_id, start_lon, start_lat, end_lon, end_lat) AS (
                    VALUES %s
                ),
                candidate_geom AS (
                    SELECT DISTINCT link_id,
                           ST_SetSRID(ST_MakeLine(
                               ST_MakePoint(start_lon::double precision, start_lat::double precision),
                               ST_MakePoint(end_lon::double precision, end_lat::double precision)
                           ), 4326) AS geom
                    FROM candidate
                )
                SELECT c.link_id, a.uid, a.ciq_code, a.zone_code, a.area_name
                FROM candidate_geom c
                JOIN lta.study_area a
                  ON a.active = TRUE
                 AND ST_Intersects(c.geom, a.geom)
                ORDER BY c.link_id, a.ciq_code,
                         CASE a.zone_code WHEN 'CORE' THEN 1 WHEN 'APPROACH' THEN 2 WHEN 'CORRIDOR' THEN 3 ELSE 9 END
                """.formatted(values);
        Map<String, List<TrafficSpeedScopeMatch>> result = new LinkedHashMap<>();
        jdbcTemplate.query(sql, rs -> {
            String linkId = rs.getString("link_id");
            result.computeIfAbsent(linkId, ignored -> new ArrayList<>()).add(scopeMatch(rs));
        }, args.toArray());
        return result;
    }

    private Map<String, LinkState> existingLinks(List<TrafficSpeedBandRecord> records) {
        String placeholders = records.stream().map(ignored -> "?").collect(Collectors.joining(","));
        String sql = """
                SELECT uid, link_id, road_name, road_category, start_lon, start_lat, end_lon, end_lat
                FROM lta.traffic_link
                WHERE link_id IN (%s)
                """.formatted(placeholders);
        return jdbcTemplate.query(sql, rs -> {
            Map<String, LinkState> states = new HashMap<>();
            while (rs.next()) {
                states.put(rs.getString("link_id"), new LinkState(
                        (UUID) rs.getObject("uid"), rs.getString("road_name"), nullableShort(rs.getObject("road_category")),
                        rs.getBigDecimal("start_lon"), rs.getBigDecimal("start_lat"),
                        rs.getBigDecimal("end_lon"), rs.getBigDecimal("end_lat")));
            }
            return states;
        }, records.stream().map(TrafficSpeedBandRecord::linkId).toArray());
    }

    private void batchUpsertLinks(List<TrafficSpeedBandRecord> records, Instant seenTime) {
        jdbcTemplate.batchUpdate("""
                INSERT INTO lta.traffic_link
                    (uid, link_id, road_name, road_category, start_lon, start_lat, end_lon, end_lat,
                     geom, first_seen_time, last_seen_time, active)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?,
                     ST_SetSRID(ST_MakeLine(ST_MakePoint(?, ?), ST_MakePoint(?, ?)), 4326),
                     ?, ?, TRUE)
                ON CONFLICT (link_id) DO UPDATE SET
                    road_name = EXCLUDED.road_name,
                    road_category = EXCLUDED.road_category,
                    start_lon = EXCLUDED.start_lon,
                    start_lat = EXCLUDED.start_lat,
                    end_lon = EXCLUDED.end_lon,
                    end_lat = EXCLUDED.end_lat,
                    geom = EXCLUDED.geom,
                    last_seen_time = EXCLUDED.last_seen_time,
                    active = TRUE
                """, records, records.size(), (PreparedStatement ps, TrafficSpeedBandRecord r) -> {
            ps.setObject(1, UUID.randomUUID());
            ps.setString(2, r.linkId());
            ps.setString(3, blankToNull(r.roadName()));
            ps.setObject(4, r.roadCategory());
            ps.setBigDecimal(5, r.startLon());
            ps.setBigDecimal(6, r.startLat());
            ps.setBigDecimal(7, r.endLon());
            ps.setBigDecimal(8, r.endLat());
            ps.setBigDecimal(9, r.startLon());
            ps.setBigDecimal(10, r.startLat());
            ps.setBigDecimal(11, r.endLon());
            ps.setBigDecimal(12, r.endLat());
            ps.setTimestamp(13, ts(seenTime));
            ps.setTimestamp(14, ts(seenTime));
        });
    }

    private Map<String, UUID> fetchLinkUids(List<TrafficSpeedBandRecord> records) {
        String placeholders = records.stream().map(ignored -> "?").collect(Collectors.joining(","));
        String sql = "SELECT link_id, uid FROM lta.traffic_link WHERE link_id IN (" + placeholders + ")";
        return jdbcTemplate.query(sql, rs -> {
            Map<String, UUID> uids = new HashMap<>();
            while (rs.next()) {
                uids.put(rs.getString("link_id"), (UUID) rs.getObject("uid"));
            }
            return uids;
        }, records.stream().map(TrafficSpeedBandRecord::linkId).toArray());
    }

    private int batchUpsertScopes(Map<String, UUID> linkUids, Map<String, List<TrafficSpeedScopeMatch>> matchesByLink) {
        List<ScopeRow> rows = new ArrayList<>();
        matchesByLink.forEach((linkId, matches) -> {
            UUID linkUid = linkUids.get(linkId);
            int primaryPriority = matches.stream().map(TrafficSpeedScopeMatch::priority)
                    .min(Comparator.naturalOrder()).orElse(99);
            for (TrafficSpeedScopeMatch match : matches) {
                rows.add(new ScopeRow(linkUid, match.areaUid(), match.priority() == primaryPriority));
            }
        });
        int[][] counts = jdbcTemplate.batchUpdate("""
                INSERT INTO lta.traffic_link_scope (link_uid, area_uid, match_method, is_primary, active)
                VALUES (?, ?, 'SPATIAL', ?, TRUE)
                ON CONFLICT (link_uid, area_uid) DO UPDATE SET
                    match_method = 'SPATIAL',
                    is_primary = EXCLUDED.is_primary,
                    active = TRUE
                """, rows, rows.size(), (ps, row) -> {
            ps.setObject(1, row.linkUid());
            ps.setObject(2, row.areaUid());
            ps.setBoolean(3, row.primary());
        });
        int total = 0;
        for (int[] chunk : counts) {
            total += chunk.length;
        }
        return total;
    }

    private int batchInsertObservations(UUID runUid, Instant snapshotTime, List<TrafficSpeedBandRecord> records,
                                        Map<String, UUID> linkUids) {
        int[][] counts = jdbcTemplate.batchUpdate("""
                INSERT INTO lta.traffic_speed_observation
                    (snapshot_time, link_uid, speed_band, minimum_speed, maximum_speed, run_uid)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (snapshot_time, link_uid) DO NOTHING
                """, records, records.size(), (ps, r) -> {
            ps.setTimestamp(1, ts(snapshotTime));
            ps.setObject(2, linkUids.get(r.linkId()));
            ps.setShort(3, r.speedBand());
            ps.setObject(4, r.minimumSpeed());
            ps.setObject(5, r.maximumSpeed());
            ps.setObject(6, runUid);
        });
        int inserted = 0;
        for (int[] chunk : counts) {
            for (int count : chunk) {
                if (count > 0) inserted += count;
            }
        }
        return inserted;
    }

    private TrafficSpeedScopeMatch scopeMatch(ResultSet rs) throws SQLException {
        return new TrafficSpeedScopeMatch(
                (UUID) rs.getObject("uid"),
                rs.getString("ciq_code"),
                rs.getString("zone_code"),
                rs.getString("area_name")
        );
    }

    private static Timestamp ts(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static Short nullableShort(Object value) {
        return value == null ? null : ((Number) value).shortValue();
    }

    /** 数据库中既有 Link 的可变业务字段快照。 */
    private record LinkState(UUID uid, String roadName, Short roadCategory, java.math.BigDecimal startLon,
                             java.math.BigDecimal startLat, java.math.BigDecimal endLon,
                             java.math.BigDecimal endLat) {
        boolean changedFrom(TrafficSpeedBandRecord record) {
            return !java.util.Objects.equals(roadName, blankToNull(record.roadName()))
                    || !java.util.Objects.equals(roadCategory, record.roadCategory())
                    || startLon.compareTo(record.startLon()) != 0
                    || startLat.compareTo(record.startLat()) != 0
                    || endLon.compareTo(record.endLon()) != 0
                    || endLat.compareTo(record.endLat()) != 0;
        }
    }

    /** traffic_link_scope 批量 UPSERT 的最小行数据。 */
    private record ScopeRow(UUID linkUid, UUID areaUid, boolean primary) {
    }
}
