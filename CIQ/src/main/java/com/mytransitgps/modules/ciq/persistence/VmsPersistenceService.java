package com.mytransitgps.modules.ciq.persistence;

import com.mytransitgps.common.util.HashUtils;
import com.mytransitgps.modules.ciq.domain.VmsRecord;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** API04 VMS / EMAS 设备与消息状态持久化。 */
@Component
@ConditionalOnProperty(prefix = "traffic.ciq.persistence", name = "database-write-enabled", havingValue = "true")
public class VmsPersistenceService {
    private static final Logger log = LoggerFactory.getLogger(VmsPersistenceService.class);

    private final JdbcTemplate jdbc;

    public VmsPersistenceService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public int persist(UUID runUid, Instant seenTime, List<VmsRecord> rows) {
        log.info("CIQ API04 [DB-P01] VMS持久化开始，runUid={}，rows={}，seenTime={}",
                runUid, rows.size(), seenTime);

        int equipmentUpserts = 0;
        int areaMatched = 0;
        int unchangedMessages = 0;
        int endedMessages = 0;
        int insertedMessages = 0;

        for (VmsRecord row : rows) {
            UUID areaUid = area(row.longitude().doubleValue(), row.latitude().doubleValue());
            if (areaUid != null) {
                areaMatched++;
            }

            jdbc.update("""
                    INSERT INTO lta.vms_equipment(
                        uid,equipment_id,latitude,longitude,geom,area_uid,
                        first_seen_time,last_seen_time,active
                    )
                    VALUES(
                        gen_random_uuid(),?,?,?,
                        ST_SetSRID(ST_MakePoint(?,?),4326),?,?,?,TRUE
                    )
                    ON CONFLICT(equipment_id) DO UPDATE SET
                        latitude=EXCLUDED.latitude,
                        longitude=EXCLUDED.longitude,
                        geom=EXCLUDED.geom,
                        area_uid=COALESCE(EXCLUDED.area_uid,lta.vms_equipment.area_uid),
                        last_seen_time=EXCLUDED.last_seen_time,
                        active=TRUE,
                        update_time=CURRENT_TIMESTAMP
                    """,
                    row.equipmentId(), row.latitude(), row.longitude(),
                    row.longitude(), row.latitude(), areaUid,
                    Timestamp.from(seenTime), Timestamp.from(seenTime));
            equipmentUpserts++;

            UUID equipmentUid = jdbc.queryForObject(
                    "SELECT uid FROM lta.vms_equipment WHERE equipment_id=?",
                    UUID.class,
                    row.equipmentId());

            String message = row.message() == null ? "" : row.message().trim();
            String messageHash = HashUtils.sha256Hex(message.getBytes(StandardCharsets.UTF_8));
            List<Map<String, Object>> active = jdbc.queryForList(
                    "SELECT uid,message_hash FROM lta.vms_message_state "
                            + "WHERE equipment_uid=? AND active=TRUE "
                            + "ORDER BY first_seen_time DESC LIMIT 1",
                    equipmentUid);

            if (!active.isEmpty() && messageHash.equals(active.get(0).get("message_hash"))) {
                jdbc.update("UPDATE lta.vms_message_state "
                                + "SET last_seen_time=?,last_run_uid=?,update_time=CURRENT_TIMESTAMP WHERE uid=?",
                        Timestamp.from(seenTime), runUid, active.get(0).get("uid"));
                unchangedMessages++;
            } else {
                if (!active.isEmpty()) {
                    jdbc.update("UPDATE lta.vms_message_state "
                                    + "SET active=FALSE,ended_time=?,last_run_uid=?,update_time=CURRENT_TIMESTAMP WHERE uid=?",
                            Timestamp.from(seenTime), runUid, active.get(0).get("uid"));
                    endedMessages++;
                }
                jdbc.update("""
                        INSERT INTO lta.vms_message_state(
                            uid,equipment_uid,message,message_hash,
                            first_seen_time,last_seen_time,active,first_run_uid,last_run_uid
                        )
                        VALUES(gen_random_uuid(),?,?,?,?,?,TRUE,?,?)
                        """,
                        equipmentUid, message, messageHash,
                        Timestamp.from(seenTime), Timestamp.from(seenTime), runUid, runUid);
                insertedMessages++;
            }
        }

        log.info("CIQ API04 [DB-P02] 设备UPSERT阶段完成，runUid={}，equipmentUpserts={}，areaMatched={}",
                runUid, equipmentUpserts, areaMatched);
        log.info("CIQ API04 [DB-P03] 消息生命周期阶段完成，runUid={}，unchanged={}，ended={}，newStates={}",
                runUid, unchangedMessages, endedMessages, insertedMessages);
        log.info("CIQ API04 [DB-P04] VMS持久化完成，runUid={}，rows={}，messageChanges={}",
                runUid, rows.size(), insertedMessages);
        return insertedMessages;
    }

    private UUID area(double lon, double lat) {
        List<UUID> matches = jdbc.query(
                "SELECT uid FROM lta.study_area "
                        + "WHERE active=TRUE "
                        + "AND ST_Intersects(ST_SetSRID(ST_MakePoint(?,?),4326),geom) "
                        + "ORDER BY CASE zone_code WHEN 'CORE' THEN 1 WHEN 'APPROACH' THEN 2 ELSE 3 END LIMIT 1",
                (rs, rowNum) -> (UUID) rs.getObject(1),
                lon, lat);
        return matches.isEmpty() ? null : matches.get(0);
    }
}
