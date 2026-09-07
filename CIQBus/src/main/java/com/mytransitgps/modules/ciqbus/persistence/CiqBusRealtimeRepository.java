package com.mytransitgps.modules.ciqbus.persistence;

import com.mytransitgps.modules.ciqbus.domain.CiqBusPassage;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CiqBusRealtimeRepository {
    private static final Logger log = LoggerFactory.getLogger(CiqBusRealtimeRepository.class);

    private final JdbcTemplate jdbc;

    public CiqBusRealtimeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public PassageWriteResult upsertPassage(CiqBusPassage p) {
        log.info("[CIQBUS-DB] 项目进度：开始写入过站明细，matchedEventId={}，routeNo={}，directionCode={}，stopCode={}，passageType={}",
                p.matchedEventId(), p.routeNo(), p.directionCode(), p.stopCode(), p.passageType());
        UUID uuid = UUID.randomUUID();
        Integer inserted = jdbc.queryForObject("""
                INSERT INTO "CIQBus".ciq_bus_stop_passage(
                    uuid, matched_event_id, route_no, operator_code, direction_code, ciq_code,
                    stop_code, stop_name, stop_sequence, passage_type, pass_time, estimated_arrival,
                    first_seen_time, last_seen_time, latitude, longitude, source_stop_code,
                    observation_count, confidence, match_method, source_name, update_time)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP)
                ON CONFLICT (matched_event_id, stop_code) DO UPDATE SET
                    last_seen_time=EXCLUDED.last_seen_time,
                    estimated_arrival=EXCLUDED.estimated_arrival,
                    latitude=EXCLUDED.latitude,
                    longitude=EXCLUDED.longitude,
                    observation_count=GREATEST("CIQBus".ciq_bus_stop_passage.observation_count, EXCLUDED.observation_count),
                    confidence=GREATEST(COALESCE("CIQBus".ciq_bus_stop_passage.confidence,0), COALESCE(EXCLUDED.confidence,0)),
                    match_method=EXCLUDED.match_method,
                    update_time=CURRENT_TIMESTAMP
                RETURNING CASE WHEN xmax = 0 THEN 1 ELSE 0 END
                """, Integer.class, uuid, p.matchedEventId(), p.routeNo(), p.operatorCode(), p.directionCode(),
                p.ciqCode(), p.stopCode(), p.stopName(), p.stopSequence(), p.passageType(), ts(p.passTime()),
                ts(p.estimatedArrival()), ts(p.firstSeenTime()), ts(p.lastSeenTime()), p.latitude(), p.longitude(),
                p.sourceStopCode(), p.observationCount(), p.confidence(), p.matchMethod(), p.sourceName());
        log.info("[CIQBUS-DB] 项目进度：过站明细写入完成，matchedEventId={}，stopCode={}，数据库新增={}",
                p.matchedEventId(), p.stopCode(), inserted != null && inserted == 1);
        return new PassageWriteResult(uuid, inserted != null && inserted == 1);
    }

    public List<CiqBusPassage> passages(String matchedEventId) {
        List<CiqBusPassage> rows = jdbc.query("""
                SELECT uuid, crossing_event_uuid, matched_event_id, route_no, operator_code, direction_code, ciq_code,
                       stop_code, stop_name, stop_sequence, passage_type, pass_time, estimated_arrival,
                       first_seen_time, last_seen_time, latitude, longitude, source_stop_code,
                       observation_count, confidence, match_method, source_name
                FROM "CIQBus".ciq_bus_stop_passage
                WHERE matched_event_id=?
                ORDER BY pass_time
                """, (rs, rowNum) -> passage(rs), matchedEventId);
        log.info("[CIQBUS-DB] 项目进度：读取过站明细完成，matchedEventId={}，记录数={}", matchedEventId, rows.size());
        return rows;
    }

    @Transactional
    public CrossingWriteResult tryCreateCrossing(String matchedEventId, ZoneId zoneId) {
        log.info("[CIQBUS-DB] 项目进度：开始尝试生成完整过境事件，matchedEventId={}", matchedEventId);
        List<CiqBusPassage> rows = passages(matchedEventId);
        CiqBusPassage entry = rows.stream().filter(p -> "ENTRY".equals(p.passageType())).findFirst().orElse(null);
        CiqBusPassage exit = rows.stream().filter(p -> "EXIT".equals(p.passageType())).findFirst().orElse(null);
        if (entry == null || exit == null || !exit.passTime().isAfter(entry.passTime())) {
            log.info("[CIQBUS-DB] 项目进度：完整过境事件条件未满足，matchedEventId={}，hasEntry={}，hasExit={}",
                    matchedEventId, entry != null, exit != null);
            return new CrossingWriteResult(null, null, false);
        }
        int seconds = (int) java.time.Duration.between(entry.passTime(), exit.passTime()).toSeconds();
        LocalDate serviceDate = LocalDate.ofInstant(entry.passTime(), zoneId);
        String fingerprint = sha256(serviceDate + "|" + entry.routeNo() + "|" + entry.directionCode() + "|" + matchedEventId
                + "|" + entry.stopCode() + "|" + entry.passTime() + "|" + exit.stopCode() + "|" + exit.passTime());
        UUID uuid = UUID.randomUUID();
        Integer inserted;
        try {
            inserted = jdbc.queryForObject("""
                INSERT INTO "CIQBus".ciq_bus_crossing_event(
                    uuid, matched_event_id, event_fingerprint, service_date, route_no, operator_code,
                    ciq_code, direction_code, source_name, entry_stop_code, exit_stop_code,
                    ciq_entry_time, ciq_exit_time, crossing_seconds, first_seen_time, last_seen_time,
                    entry_latitude, entry_longitude, exit_latitude, exit_longitude,
                    observation_count, confidence, match_method, event_status, update_time)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP)
                ON CONFLICT (event_fingerprint) DO NOTHING
                RETURNING 1
                """, Integer.class, uuid, matchedEventId, fingerprint, serviceDate, entry.routeNo(), entry.operatorCode(),
                    entry.ciqCode(), entry.directionCode(), entry.sourceName(), entry.stopCode(), exit.stopCode(),
                    ts(entry.passTime()), ts(exit.passTime()), seconds, ts(entry.firstSeenTime()), ts(exit.lastSeenTime()),
                    entry.latitude(), entry.longitude(), exit.latitude(), exit.longitude(),
                    entry.observationCount() + exit.observationCount(), Math.min(entry.confidence(), exit.confidence()),
                    "GPS_ETA_SEQUENCE", seconds > 0 ? "CONFIRMED" : "REJECTED");
        } catch (EmptyResultDataAccessException ex) {
            inserted = null;
        }
        if (inserted == null) {
            log.info("[CIQBUS-DB] 项目进度：完整过境事件已存在，matchedEventId={}，fingerprint={}", matchedEventId, fingerprint);
            return new CrossingWriteResult(null, fingerprint, false);
        }
        jdbc.update("""
                UPDATE "CIQBus".ciq_bus_stop_passage
                SET crossing_event_uuid=?, update_time=CURRENT_TIMESTAMP
                WHERE matched_event_id=? AND stop_code IN (?,?)
                """, uuid, matchedEventId, entry.stopCode(), exit.stopCode());
        log.info("[CIQBUS-DB] 项目进度：完整过境事件写入完成，matchedEventId={}，crossingUuid={}，crossingSeconds={}",
                matchedEventId, uuid, seconds);
        return new CrossingWriteResult(uuid, fingerprint, true);
    }

    public int upsertDailySummary(LocalDate date) {
        log.info("[CIQBUS-DB] 项目进度：开始汇总CIQBus每日统计，serviceDate={}", date);
        int rows = jdbc.update("""
                INSERT INTO "CIQBus".ciq_bus_daily_summary(
                    service_date, ciq_code, route_no, direction_code, crossing_count,
                    avg_crossing_seconds, median_crossing_seconds, p90_crossing_seconds,
                    p95_crossing_seconds, min_crossing_seconds, max_crossing_seconds,
                    confirmed_count, incomplete_count, update_time)
                SELECT service_date, ciq_code, route_no, direction_code, COUNT(*),
                       AVG(crossing_seconds),
                       percentile_cont(0.5) WITHIN GROUP (ORDER BY crossing_seconds),
                       percentile_cont(0.9) WITHIN GROUP (ORDER BY crossing_seconds),
                       percentile_cont(0.95) WITHIN GROUP (ORDER BY crossing_seconds),
                       MIN(crossing_seconds), MAX(crossing_seconds),
                       COUNT(*) FILTER (WHERE event_status='CONFIRMED'),
                       COUNT(*) FILTER (WHERE event_status='INCOMPLETE'),
                       CURRENT_TIMESTAMP
                FROM "CIQBus".ciq_bus_crossing_event
                WHERE service_date=? AND crossing_seconds IS NOT NULL
                GROUP BY service_date, ciq_code, route_no, direction_code
                ON CONFLICT (service_date, ciq_code, route_no, direction_code) DO UPDATE SET
                    crossing_count=EXCLUDED.crossing_count,
                    avg_crossing_seconds=EXCLUDED.avg_crossing_seconds,
                    median_crossing_seconds=EXCLUDED.median_crossing_seconds,
                    p90_crossing_seconds=EXCLUDED.p90_crossing_seconds,
                    p95_crossing_seconds=EXCLUDED.p95_crossing_seconds,
                    min_crossing_seconds=EXCLUDED.min_crossing_seconds,
                    max_crossing_seconds=EXCLUDED.max_crossing_seconds,
                    confirmed_count=EXCLUDED.confirmed_count,
                    incomplete_count=EXCLUDED.incomplete_count,
                    update_time=CURRENT_TIMESTAMP
                """, date);
        log.info("[CIQBUS-DB] 项目进度：CIQBus每日统计汇总完成，serviceDate={}，影响行数={}", date, rows);
        return rows;
    }

    public List<CiqBusPassage> passagesForDate(LocalDate date, ZoneId zoneId) {
        log.info("[CIQBUS-DB] 项目进度：开始读取日期过站明细，serviceDate={}", date);
        Instant start = date.atStartOfDay(zoneId).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(zoneId).toInstant();
        List<CiqBusPassage> rows = jdbc.query("""
                SELECT uuid, crossing_event_uuid, matched_event_id, route_no, operator_code, direction_code, ciq_code,
                       stop_code, stop_name, stop_sequence, passage_type, pass_time, estimated_arrival,
                       first_seen_time, last_seen_time, latitude, longitude, source_stop_code,
                       observation_count, confidence, match_method, source_name
                FROM "CIQBus".ciq_bus_stop_passage
                WHERE pass_time >= ? AND pass_time < ?
                ORDER BY pass_time, matched_event_id, stop_sequence
                """, (rs, rowNum) -> passage(rs), ts(start), ts(end));
        log.info("[CIQBUS-DB] 项目进度：日期过站明细读取完成，serviceDate={}，记录数={}", date, rows.size());
        return rows;
    }

    public List<CrossingRow> crossingsForDate(LocalDate date) {
        log.info("[CIQBUS-DB] 项目进度：开始读取日期完整过境事件，serviceDate={}", date);
        List<CrossingRow> rows = jdbc.query("""
                SELECT matched_event_id, route_no, direction_code, entry_stop_code, exit_stop_code,
                       ciq_entry_time, ciq_exit_time, crossing_seconds, observation_count,
                       confidence, event_status
                FROM "CIQBus".ciq_bus_crossing_event
                WHERE service_date=?
                ORDER BY ciq_entry_time
                """, (rs, n) -> new CrossingRow(rs.getString("matched_event_id"), rs.getString("route_no"),
                rs.getString("direction_code"), rs.getString("entry_stop_code"), rs.getString("exit_stop_code"),
                inst(rs, "ciq_entry_time"), inst(rs, "ciq_exit_time"), rs.getInt("crossing_seconds"),
                rs.getInt("observation_count"), rs.getDouble("confidence"), rs.getString("event_status")), date);
        log.info("[CIQBUS-DB] 项目进度：日期完整过境事件读取完成，serviceDate={}，记录数={}", date, rows.size());
        return rows;
    }

    private CiqBusPassage passage(ResultSet rs) throws SQLException {
        return new CiqBusPassage((UUID) rs.getObject("uuid"), (UUID) rs.getObject("crossing_event_uuid"),
                rs.getString("matched_event_id"), rs.getString("route_no"), rs.getString("operator_code"),
                rs.getString("direction_code"), rs.getString("ciq_code"), rs.getString("stop_code"),
                rs.getString("stop_name"), (Integer) rs.getObject("stop_sequence"), rs.getString("passage_type"),
                inst(rs, "pass_time"), inst(rs, "estimated_arrival"), inst(rs, "first_seen_time"),
                inst(rs, "last_seen_time"), dbl(rs, "latitude"), dbl(rs, "longitude"),
                rs.getString("source_stop_code"), rs.getInt("observation_count"), rs.getDouble("confidence"),
                rs.getString("match_method"), rs.getString("source_name"));
    }

    private static Timestamp ts(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private static Instant inst(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts == null ? null : ts.toInstant();
    }

    private static Double dbl(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        return value == null ? null : ((Number) value).doubleValue();
    }

    static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    public record PassageWriteResult(UUID uuid, boolean inserted) {
    }

    public record CrossingWriteResult(UUID uuid, String fingerprint, boolean inserted) {
    }

    public record CrossingRow(String matchedEventId, String routeNo, String directionCode,
                              String entryStopCode, String exitStopCode, Instant entryTime,
                              Instant exitTime, int crossingSeconds, int observationCount,
                              double confidence, String eventStatus) {
    }
}
