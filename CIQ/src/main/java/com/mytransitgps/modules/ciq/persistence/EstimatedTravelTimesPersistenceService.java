package com.mytransitgps.modules.ciq.persistence;

import com.mytransitgps.modules.ciq.domain.EstimatedTravelTimeRecord;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** API02 Estimated Travel Times PostgreSQL persistence. */
@Component
@ConditionalOnProperty(prefix = "traffic.ciq.persistence", name = "database-write-enabled", havingValue = "true")
public class EstimatedTravelTimesPersistenceService {
    private static final Logger log = LoggerFactory.getLogger(EstimatedTravelTimesPersistenceService.class);
    private final JdbcTemplate jdbc;

    public EstimatedTravelTimesPersistenceService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public int persist(UUID runUid, Instant snapshotTime, List<EstimatedTravelTimeRecord> rows) {
        log.info("CIQ API02 [DB-P01] travel_time开始持久化，runUid={}，rows={}，snapshotTime={}",
                runUid, rows.size(), snapshotTime);
        int inserted = 0;
        for (EstimatedTravelTimeRecord row : rows) {
            jdbc.update("""
                    INSERT INTO lta.travel_time_segment
                    (uid,name,direction,far_end_point,start_point,end_point,first_seen_time,last_seen_time,active)
                    VALUES(gen_random_uuid(),?,?,?,?,?,?,?,TRUE)
                    ON CONFLICT(name,direction,far_end_point,start_point,end_point)
                    DO UPDATE SET last_seen_time=EXCLUDED.last_seen_time,
                                  active=TRUE,
                                  update_time=CURRENT_TIMESTAMP
                    """,
                    row.name(), row.direction(), row.farEndPoint(), row.startPoint(), row.endPoint(),
                    Timestamp.from(snapshotTime), Timestamp.from(snapshotTime));

            UUID segmentUid = jdbc.queryForObject("""
                    SELECT uid FROM lta.travel_time_segment
                    WHERE name=? AND direction=? AND far_end_point=? AND start_point=? AND end_point=?
                    """, UUID.class,
                    row.name(), row.direction(), row.farEndPoint(), row.startPoint(), row.endPoint());

            inserted += jdbc.update("""
                    INSERT INTO lta.travel_time_observation
                    (uid,snapshot_time,segment_uid,est_time_min,run_uid)
                    VALUES(gen_random_uuid(),?,?,?,?)
                    ON CONFLICT(snapshot_time,segment_uid) DO NOTHING
                    """, Timestamp.from(snapshotTime), segmentUid, row.estTime(), runUid);
        }
        log.info("CIQ API02 [DB-P02] travel_time持久化完成，runUid={}，rows={}，observationsInserted={}",
                runUid, rows.size(), inserted);
        return inserted;
    }
}
