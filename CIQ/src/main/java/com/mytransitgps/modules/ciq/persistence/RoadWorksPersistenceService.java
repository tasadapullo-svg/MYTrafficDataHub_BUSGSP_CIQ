package com.mytransitgps.modules.ciq.persistence;

import com.mytransitgps.modules.ciq.domain.RoadWorkRecord;
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

/** API06 已批准道路施工事件生命周期持久化。 */
@Component
@ConditionalOnProperty(prefix = "traffic.ciq.persistence", name = "database-write-enabled", havingValue = "true")
public class RoadWorksPersistenceService {
    private static final Logger log = LoggerFactory.getLogger(RoadWorksPersistenceService.class);

    private final JdbcTemplate jdbc;

    public RoadWorksPersistenceService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public int persist(UUID runUid, Instant seenTime, List<RoadWorkRecord> rows) {
        log.info("CIQ API06 [DB-P01] 道路施工生命周期持久化开始，runUid={}，activeSnapshotRows={}，seenTime={}",
                runUid, rows.size(), seenTime);

        Set<String> currentEventIds = new HashSet<>();
        int upsertAffected = 0;
        for (RoadWorkRecord row : rows) {
            currentEventIds.add(row.eventId());
            upsertAffected += jdbc.update("""
                    INSERT INTO lta.road_work_event(
                        uid,event_id,start_date,end_date,svc_dept,road_name,other,
                        first_seen_time,last_seen_time,active,first_run_uid,last_run_uid
                    )
                    VALUES(gen_random_uuid(),?,?,?,?,?,?,?,?,TRUE,?,?)
                    ON CONFLICT(event_id) DO UPDATE SET
                        start_date=EXCLUDED.start_date,
                        end_date=EXCLUDED.end_date,
                        svc_dept=EXCLUDED.svc_dept,
                        road_name=EXCLUDED.road_name,
                        other=EXCLUDED.other,
                        last_seen_time=EXCLUDED.last_seen_time,
                        resolved_time=NULL,
                        active=TRUE,
                        last_run_uid=EXCLUDED.last_run_uid,
                        update_time=CURRENT_TIMESTAMP
                    """,
                    row.eventId(), row.startDate(), row.endDate(), row.svcDept(), row.roadName(), row.other(),
                    Timestamp.from(seenTime), Timestamp.from(seenTime), runUid, runUid);
        }
        log.info("CIQ API06 [DB-P02] 当前道路施工UPSERT完成，runUid={}，snapshotIds={}，upsertAffected={}",
                runUid, currentEventIds.size(), upsertAffected);

        int resolved = 0;
        List<String> activeIds = jdbc.query(
                "SELECT event_id FROM lta.road_work_event WHERE active=TRUE",
                (rs, rowNum) -> rs.getString(1));
        for (String eventId : activeIds) {
            if (!currentEventIds.contains(eventId)) {
                resolved += jdbc.update("UPDATE lta.road_work_event "
                                + "SET active=FALSE,resolved_time=?,last_run_uid=?,update_time=CURRENT_TIMESTAMP "
                                + "WHERE event_id=?",
                        Timestamp.from(seenTime), runUid, eventId);
            }
        }
        log.info("CIQ API06 [DB-P03] 已消失施工事件解析完成，runUid={}，dbActiveBefore={}，resolved={}",
                runUid, activeIds.size(), resolved);
        log.info("CIQ API06 [DB-P04] 道路施工持久化完成，runUid={}，rows={}，upsertAffected={}，resolved={}",
                runUid, rows.size(), upsertAffected, resolved);
        return upsertAffected;
    }
}
