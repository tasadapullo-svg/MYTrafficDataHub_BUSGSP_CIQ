package com.mytransitgps.modules.ciqbus.dashboard.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** CIQBus Dashboard 专用 PostgreSQL 只读访问层。 */
@Repository
@ConditionalOnProperty(prefix = "mytransitgps.database", name = "enabled", havingValue = "true")
public class CiqBusDashboardRepository {
    private final JdbcTemplate jdbc;

    public CiqBusDashboardRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public OverviewRow overview(LocalDate date) {
        return jdbc.queryForObject("""
                SELECT
                  count(*) FILTER (WHERE event_status='CONFIRMED' AND direction_code='SG_TO_JB') AS sg_to_jb,
                  count(*) FILTER (WHERE event_status='CONFIRMED' AND direction_code='JB_TO_SG') AS jb_to_sg,
                  count(*) FILTER (WHERE event_status='CONFIRMED') AS total_confirmed,
                  count(*) FILTER (WHERE event_status='INCOMPLETE') AS incomplete_count,
                  avg(crossing_seconds) FILTER (WHERE event_status='CONFIRMED' AND crossing_seconds IS NOT NULL)::double precision AS avg_seconds,
                  percentile_cont(0.5) WITHIN GROUP (ORDER BY crossing_seconds) FILTER (WHERE event_status='CONFIRMED' AND crossing_seconds IS NOT NULL) AS median_seconds,
                  percentile_cont(0.9) WITHIN GROUP (ORDER BY crossing_seconds) FILTER (WHERE event_status='CONFIRMED' AND crossing_seconds IS NOT NULL) AS p90_seconds,
                  percentile_cont(0.95) WITHIN GROUP (ORDER BY crossing_seconds) FILTER (WHERE event_status='CONFIRMED' AND crossing_seconds IS NOT NULL) AS p95_seconds
                FROM "CIQBus".ciq_bus_crossing_event
                WHERE service_date=?
                """, (rs, n) -> new OverviewRow(
                rs.getLong("sg_to_jb"), rs.getLong("jb_to_sg"), rs.getLong("total_confirmed"),
                rs.getLong("incomplete_count"), nullableDouble(rs, "avg_seconds"),
                nullableDouble(rs, "median_seconds"), nullableDouble(rs, "p90_seconds"),
                nullableDouble(rs, "p95_seconds")), date);
    }

    public Map<String, Long> routeCounts(LocalDate date) {
        Map<String, Long> result = new LinkedHashMap<>();
        jdbc.query("""
                SELECT route_no, count(*) AS crossing_count
                FROM "CIQBus".ciq_bus_crossing_event
                WHERE service_date=? AND event_status='CONFIRMED'
                GROUP BY route_no
                ORDER BY route_no
                """, (org.springframework.jdbc.core.RowCallbackHandler) rs ->
                        result.put(rs.getString("route_no"), rs.getLong("crossing_count")), date);
        return result;
    }

    public List<HourlyRow> hourly(LocalDate date, ZoneId zoneId) {
        return jdbc.query("""
                SELECT EXTRACT(HOUR FROM ciq_entry_time AT TIME ZONE ?)::int AS hour_of_day,
                       count(*) FILTER (WHERE direction_code='SG_TO_JB') AS sg_to_jb,
                       count(*) FILTER (WHERE direction_code='JB_TO_SG') AS jb_to_sg,
                       count(*) AS total,
                       avg(crossing_seconds) FILTER (WHERE crossing_seconds IS NOT NULL)::double precision AS avg_seconds,
                       percentile_cont(0.5) WITHIN GROUP (ORDER BY crossing_seconds) FILTER (WHERE crossing_seconds IS NOT NULL) AS median_seconds,
                       percentile_cont(0.9) WITHIN GROUP (ORDER BY crossing_seconds) FILTER (WHERE crossing_seconds IS NOT NULL) AS p90_seconds,
                       percentile_cont(0.95) WITHIN GROUP (ORDER BY crossing_seconds) FILTER (WHERE crossing_seconds IS NOT NULL) AS p95_seconds
                FROM "CIQBus".ciq_bus_crossing_event
                WHERE service_date=? AND event_status='CONFIRMED' AND ciq_entry_time IS NOT NULL
                GROUP BY hour_of_day
                ORDER BY hour_of_day
                """, (rs, n) -> new HourlyRow(rs.getInt("hour_of_day"), rs.getLong("sg_to_jb"),
                rs.getLong("jb_to_sg"), rs.getLong("total"), nullableDouble(rs, "avg_seconds"),
                nullableDouble(rs, "median_seconds"), nullableDouble(rs, "p90_seconds"),
                nullableDouble(rs, "p95_seconds")), zoneId.getId(), date);
    }

    public long countPassages(LocalDate date, ZoneId zoneId, String route, String direction) {
        Query query = passageWhere(date, zoneId, route, direction, "SELECT count(*) FROM \"CIQBus\".ciq_bus_stop_passage WHERE pass_time>=? AND pass_time<?");
        Long value = jdbc.queryForObject(query.sql(), Long.class, query.args().toArray());
        return value == null ? 0 : value;
    }

    public List<PassageRow> passages(LocalDate date, ZoneId zoneId, String route, String direction, int limit, long offset) {
        Query query = passageWhere(date, zoneId, route, direction, """
                SELECT matched_event_id,route_no,operator_code,direction_code,stop_code,stop_name,
                       pass_time,estimated_arrival,latitude,longitude,observation_count,confidence,match_method,create_time
                FROM "CIQBus".ciq_bus_stop_passage
                WHERE pass_time>=? AND pass_time<?
                """);
        String sql = query.sql() + " ORDER BY pass_time DESC, matched_event_id DESC LIMIT ? OFFSET ?";
        List<Object> args = new ArrayList<>(query.args());
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql, (rs, n) -> new PassageRow(
                rs.getString("matched_event_id"), rs.getString("route_no"), rs.getString("operator_code"),
                rs.getString("direction_code"), rs.getString("stop_code"), rs.getString("stop_name"),
                instant(rs, "pass_time"), instant(rs, "estimated_arrival"), nullableDouble(rs, "latitude"),
                nullableDouble(rs, "longitude"), rs.getInt("observation_count"), nullableDouble(rs, "confidence"),
                rs.getString("match_method"), instant(rs, "create_time")), args.toArray());
    }

    public long countCrossings(LocalDate date, String route, String direction) {
        Query query = crossingWhere(date, route, direction, "SELECT count(*) FROM \"CIQBus\".ciq_bus_crossing_event WHERE service_date=?");
        Long value = jdbc.queryForObject(query.sql(), Long.class, query.args().toArray());
        return value == null ? 0 : value;
    }

    public List<CrossingRow> crossings(LocalDate date, String route, String direction, int limit, long offset) {
        Query query = crossingWhere(date, route, direction, """
                SELECT matched_event_id,route_no,direction_code,entry_stop_code,ciq_entry_time,
                       exit_stop_code,ciq_exit_time,crossing_seconds,confidence,event_status
                FROM "CIQBus".ciq_bus_crossing_event
                WHERE service_date=?
                """);
        String sql = query.sql() + " ORDER BY ciq_entry_time DESC NULLS LAST, matched_event_id DESC LIMIT ? OFFSET ?";
        List<Object> args = new ArrayList<>(query.args());
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql, (rs, n) -> new CrossingRow(
                rs.getString("matched_event_id"), rs.getString("route_no"), rs.getString("direction_code"),
                rs.getString("entry_stop_code"), instant(rs, "ciq_entry_time"), rs.getString("exit_stop_code"),
                instant(rs, "ciq_exit_time"), nullableInteger(rs, "crossing_seconds"),
                nullableDouble(rs, "confidence"), rs.getString("event_status")), args.toArray());
    }

    public Instant latestActivity() {
        return jdbc.queryForObject("""
                SELECT max(activity_time) FROM (
                  SELECT max(last_seen_time) AS activity_time FROM "CIQBus".ciq_bus_stop_passage
                  UNION ALL
                  SELECT max(COALESCE(ciq_exit_time,ciq_entry_time,last_seen_time)) AS activity_time FROM "CIQBus".ciq_bus_crossing_event
                ) x
                """, (rs, n) -> instant(rs, 1));
    }

    private Query passageWhere(LocalDate date, ZoneId zoneId, String route, String direction, String base) {
        Instant start = date.atStartOfDay(zoneId).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(zoneId).toInstant();
        StringBuilder sql = new StringBuilder(base);
        List<Object> args = new ArrayList<>();
        args.add(Timestamp.from(start));
        args.add(Timestamp.from(end));
        appendFilters(sql, args, route, direction);
        return new Query(sql.toString(), args);
    }

    private Query crossingWhere(LocalDate date, String route, String direction, String base) {
        StringBuilder sql = new StringBuilder(base);
        List<Object> args = new ArrayList<>();
        args.add(date);
        appendFilters(sql, args, route, direction);
        return new Query(sql.toString(), args);
    }

    private void appendFilters(StringBuilder sql, List<Object> args, String route, String direction) {
        if (route != null && !route.equals("ALL")) {
            sql.append(" AND route_no=?");
            args.add(route);
        }
        if (direction != null && !direction.equals("ALL")) {
            sql.append(" AND direction_code=?");
            args.add(direction);
        }
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private static Instant instant(ResultSet rs, int column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private static Double nullableDouble(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        return value == null ? null : ((Number) value).doubleValue();
    }

    private static Integer nullableInteger(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        return value == null ? null : ((Number) value).intValue();
    }

    public record OverviewRow(long sgToJb, long jbToSg, long total, long incompleteCount,
                              Double avgSeconds, Double medianSeconds, Double p90Seconds, Double p95Seconds) { }
    public record HourlyRow(int hour, long sgToJb, long jbToSg, long total,
                            Double avgSeconds, Double medianSeconds, Double p90Seconds, Double p95Seconds) { }
    public record PassageRow(String matchedEventId, String routeNo, String operatorCode, String directionCode,
                             String stopCode, String stopName, Instant passTime, Instant estimatedArrival,
                             Double latitude, Double longitude, int observationCount, Double confidence,
                             String matchMethod, Instant createTime) { }
    public record CrossingRow(String matchedEventId, String routeNo, String directionCode,
                              String entryStopCode, Instant entryTime, String exitStopCode, Instant exitTime,
                              Integer crossingSeconds, Double confidence, String eventStatus) { }
    private record Query(String sql, List<Object> args) { }
}
