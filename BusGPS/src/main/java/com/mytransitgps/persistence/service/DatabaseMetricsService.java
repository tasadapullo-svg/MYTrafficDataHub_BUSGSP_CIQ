package com.mytransitgps.persistence.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 中文名称：数据库基线与尺寸读取服务。
 *
 * 功能说明：只执行 SELECT，记录正式采集前后的核心表行数及 PostgreSQL 关系尺寸。
 */
@Service
@ConditionalOnProperty(prefix = "mytransitgps.database", name = "enabled", havingValue = "true")
public class DatabaseMetricsService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMetricsService.class);
    public static final List<String> TABLES = List.of(
            "core.collection_run", "jb.api_request_log", "jb.realtime_snapshot", "jb.vehicle_observation",
            "jb.vehicle_observation_qc", "jb.vehicle_latest_state", "jb.static_version", "jb.static_route",
            "jb.static_trip", "jb.static_stop", "jb.static_stop_time", "jb.static_shape", "jb.static_shape_point");
    private final JdbcTemplate jdbc;

    public DatabaseMetricsService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Map<String, Object> snapshot() {
        log.info("开始采集数据库基线与关系尺寸，tableCount={}", TABLES.size());
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Long> counts = new LinkedHashMap<>();
        Map<String, Map<String, Long>> sizes = new LinkedHashMap<>();
        for (String table : TABLES) {
            counts.put(table, jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class));
            Map<String, Long> row = new LinkedHashMap<>();
            row.put("table_bytes", jdbc.queryForObject("SELECT pg_relation_size(?)", Long.class, table));
            row.put("index_bytes", jdbc.queryForObject("SELECT pg_indexes_size(?)", Long.class, table));
            row.put("total_bytes", jdbc.queryForObject("SELECT pg_total_relation_size(?)", Long.class, table));
            sizes.put(table, row);
        }
        result.put("database", "postgres");
        result.put("database_bytes", jdbc.queryForObject("SELECT pg_database_size(current_database())", Long.class));
        result.put("counts", counts); result.put("relations", sizes);
        log.info("数据库基线与关系尺寸采集完成，tableCount={}，databaseBytes={}", TABLES.size(), result.get("database_bytes"));
        return result;
    }
}
