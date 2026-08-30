package com.mytransitgps.modules.ciq.persistence;

import com.mytransitgps.common.util.HashUtils;
import com.mytransitgps.modules.ciq.domain.TrafficIncidentRecord;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** API03 Traffic Incidents lifecycle persistence. */
@Component
@ConditionalOnProperty(prefix = "traffic.ciq.persistence", name = "database-write-enabled", havingValue = "true")
public class TrafficIncidentsPersistenceService {
    private static final Logger log = LoggerFactory.getLogger(TrafficIncidentsPersistenceService.class);
    private final JdbcTemplate jdbc;

    public TrafficIncidentsPersistenceService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public int persist(UUID runUid, Instant seenTime, List<TrafficIncidentRecord> rows) {
        log.info("CIQ API03 [DB-P01] incident生命周期持久化开始，runUid={}，activeSnapshotRows={}",
                runUid, rows.size());
        Set<String> seenHashes = new HashSet<>();
        int affected = 0;
        for (TrafficIncidentRecord row : rows) {
            String hash = HashUtils.sha256Hex((normalize(row.type()) + "|" + row.latitude() + "|"
                    + row.longitude() + "|" + normalize(row.message())).getBytes(StandardCharsets.UTF_8));
            seenHashes.add(hash);
            UUID areaUid = area(row.longitude().doubleValue(), row.latitude().doubleValue());
            affected += jdbc.update("""
                    INSERT INTO lta.traffic_incident_event
                    (uid,event_fingerprint,type,latitude,longitude,geom,message,area_uid,
                     first_seen_time,last_seen_time,active,first_run_uid,last_run_uid)
                    VALUES(gen_random_uuid(),?,?,?,?,ST_SetSRID(ST_MakePoint(?,?),4326),?,?,?,?,TRUE,?,?)
                    ON CONFLICT(event_fingerprint) DO UPDATE SET
                        last_seen_time=EXCLUDED.last_seen_time,
                        resolved_time=NULL,
                        active=TRUE,
                        last_run_uid=EXCLUDED.last_run_uid,
                        area_uid=COALESCE(EXCLUDED.area_uid,lta.traffic_incident_event.area_uid),
                        update_time=CURRENT_TIMESTAMP
                    """,
                    hash, row.type(), row.latitude(), row.longitude(), row.longitude(), row.latitude(),
                    row.message(), areaUid, Timestamp.from(seenTime), Timestamp.from(seenTime), runUid, runUid);
        }

        int resolved = 0;
        for (String hash : jdbc.query(
                "SELECT event_fingerprint FROM lta.traffic_incident_event WHERE active=TRUE",
                (rs, rowNum) -> rs.getString(1))) {
            if (!seenHashes.contains(hash)) {
                resolved += jdbc.update("""
                        UPDATE lta.traffic_incident_event
                        SET active=FALSE,resolved_time=?,last_run_uid=?,update_time=CURRENT_TIMESTAMP
                        WHERE event_fingerprint=? AND active=TRUE
                        """, Timestamp.from(seenTime), runUid, hash);
            }
        }
        log.info("CIQ API03 [DB-P02] incident生命周期持久化完成，runUid={}，upsertAffected={}，resolved={}",
                runUid, affected, resolved);
        return affected;
    }

    private UUID area(double lon, double lat) {
        List<UUID> matches = jdbc.query("""
                SELECT uid FROM lta.study_area
                WHERE active=TRUE
                  AND ST_Intersects(ST_SetSRID(ST_MakePoint(?,?),4326),geom)
                ORDER BY CASE zone_code WHEN 'CORE' THEN 1 WHEN 'APPROACH' THEN 2 ELSE 3 END
                LIMIT 1
                """, (rs, rowNum) -> (UUID) rs.getObject(1), lon, lat);
        return matches.isEmpty() ? null : matches.get(0);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
}
