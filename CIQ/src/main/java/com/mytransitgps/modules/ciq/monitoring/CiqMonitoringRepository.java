package com.mytransitgps.modules.ciq.monitoring;

import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

/**
 * CIQ Dashboard 只读监控查询仓库。
 *
 * <p>所有 SQL 限定在 `lta` schema 的 CIQ/API01 审计与观测表，不复用或污染 BUS GPS
 * `DashboardRepository`。</p>
 */
@Repository
@ConditionalOnBean(JdbcTemplate.class)
public class CiqMonitoringRepository {
    private final JdbcTemplate jdbcTemplate;

    public CiqMonitoringRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Instant lastCollectionTime() {
        return jdbcTemplate.query("""
                SELECT MAX(request_end_time)
                FROM lta.collection_run r
                JOIN lta.api_endpoint e ON e.uid = r.api_endpoint_uid
                WHERE e.api_code = 'API01'
                """, rs -> rs.next() ? rs.getTimestamp(1) == null ? null : rs.getTimestamp(1).toInstant() : null);
    }

    public long todayRecords() {
        return count("""
                SELECT COUNT(*)
                FROM lta.traffic_speed_observation
                WHERE snapshot_time >= CURRENT_DATE
                """);
    }

    public long yesterdayRecords() {
        return count("""
                SELECT COUNT(*)
                FROM lta.traffic_speed_observation
                WHERE snapshot_time >= CURRENT_DATE - INTERVAL '1 day'
                  AND snapshot_time < CURRENT_DATE
                """);
    }

    public long requestCount() {
        return countRuns(null);
    }

    public long successCount() {
        return countRuns(Boolean.TRUE);
    }

    public long failureCount() {
        return countRuns(Boolean.FALSE);
    }

    public Instant latestDataTime() {
        return jdbcTemplate.query("SELECT MAX(snapshot_time) FROM lta.traffic_speed_observation",
                rs -> rs.next() ? rs.getTimestamp(1) == null ? null : rs.getTimestamp(1).toInstant() : null);
    }

    private long countRuns(Boolean success) {
        String sql = """
                SELECT COUNT(*)
                FROM lta.collection_run r
                JOIN lta.api_endpoint e ON e.uid = r.api_endpoint_uid
                WHERE e.api_code = 'API01'
                """ + (success == null ? "" : " AND r.success = ?");
        Long value = success == null
                ? jdbcTemplate.queryForObject(sql, Long.class)
                : jdbcTemplate.queryForObject(sql, Long.class, success);
        return value == null ? 0 : value;
    }

    private long count(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }
}
