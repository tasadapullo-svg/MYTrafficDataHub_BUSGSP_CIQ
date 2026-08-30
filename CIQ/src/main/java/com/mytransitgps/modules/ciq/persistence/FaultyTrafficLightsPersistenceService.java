package com.mytransitgps.modules.ciq.persistence;

import com.mytransitgps.modules.ciq.domain.FaultyTrafficLightRecord;
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

/** API05 故障交通信号灯事件生命周期持久化。 */
@Component
@ConditionalOnProperty(prefix = "traffic.ciq.persistence", name = "database-write-enabled", havingValue = "true")
public class FaultyTrafficLightsPersistenceService {
    private static final Logger log = LoggerFactory.getLogger(FaultyTrafficLightsPersistenceService.class);

    private final JdbcTemplate jdbc;

    public FaultyTrafficLightsPersistenceService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public int persist(UUID runUid, Instant seenTime, List<FaultyTrafficLightRecord> rows) {
        log.info("CIQ API05 [DB-P01] 故障信号灯生命周期持久化开始，runUid={}，activeSnapshotRows={}，seenTime={}",
                runUid, rows.size(), seenTime);

        Set<String> currentAlarmIds = new HashSet<>();
        int upsertAffected = 0;
        for (FaultyTrafficLightRecord row : rows) {
            currentAlarmIds.add(row.alarmId());
            upsertAffected += jdbc.update("""
                    INSERT INTO lta.faulty_traffic_light_event(
                        uid,alarm_id,node_id,fault_type,source_start_time,source_end_time,message,
                        first_seen_time,last_seen_time,active,first_run_uid,last_run_uid
                    )
                    VALUES(gen_random_uuid(),?,?,?,?,?,?,?,?,TRUE,?,?)
                    ON CONFLICT(alarm_id) DO UPDATE SET
                        node_id=EXCLUDED.node_id,
                        fault_type=EXCLUDED.fault_type,
                        source_start_time=EXCLUDED.source_start_time,
                        source_end_time=EXCLUDED.source_end_time,
                        message=EXCLUDED.message,
                        last_seen_time=EXCLUDED.last_seen_time,
                        resolved_time=NULL,
                        active=TRUE,
                        last_run_uid=EXCLUDED.last_run_uid,
                        update_time=CURRENT_TIMESTAMP
                    """,
                    row.alarmId(), row.nodeId(), row.type(),
                    Timestamp.from(row.startDate()),
                    row.endDate() == null ? null : Timestamp.from(row.endDate()),
                    row.message(), Timestamp.from(seenTime), Timestamp.from(seenTime), runUid, runUid);
        }
        log.info("CIQ API05 [DB-P02] 当前故障UPSERT完成，runUid={}，snapshotIds={}，upsertAffected={}",
                runUid, currentAlarmIds.size(), upsertAffected);

        int resolved = 0;
        List<String> activeIds = jdbc.query(
                "SELECT alarm_id FROM lta.faulty_traffic_light_event WHERE active=TRUE",
                (rs, rowNum) -> rs.getString(1));
        for (String alarmId : activeIds) {
            if (!currentAlarmIds.contains(alarmId)) {
                resolved += jdbc.update("UPDATE lta.faulty_traffic_light_event "
                                + "SET active=FALSE,resolved_time=?,last_run_uid=?,update_time=CURRENT_TIMESTAMP "
                                + "WHERE alarm_id=?",
                        Timestamp.from(seenTime), runUid, alarmId);
            }
        }
        log.info("CIQ API05 [DB-P03] 消失故障解析完成，runUid={}，dbActiveBefore={}，resolved={}",
                runUid, activeIds.size(), resolved);
        log.info("CIQ API05 [DB-P04] 故障信号灯持久化完成，runUid={}，rows={}，upsertAffected={}，resolved={}",
                runUid, rows.size(), upsertAffected, resolved);
        return upsertAffected;
    }
}
