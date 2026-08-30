package com.mytransitgps.persistence.service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * PostgreSQL、PostGIS 与 BUS GPS 基础 Schema 启动检查服务。
 *
 * <p>正式连续采集前验证数据库连接、PostGIS 以及 core/jb/kuching/kl/melaka 基础结构。
 * CIQ 的 lta Schema 不在此处强耦合校验，避免 CIQ 故障阻断 BUS GPS。
 */
@Service
@ConditionalOnProperty(prefix = "mytransitgps.database", name = "enabled", havingValue = "true")
public class DatabaseConnectionService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConnectionService.class);
    private final DataSource dataSource;

    public DatabaseConnectionService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DatabaseInfo verify() {
        log.info("开始检查PostgreSQL数据库连接与PostGIS基础能力");
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            String database;
            String user;
            String postgres;
            String timezone;
            String postgis;
            try (ResultSet rs = statement.executeQuery(
                    "select current_database(), current_user, version(), current_setting('TimeZone')")) {
                rs.next();
                database = rs.getString(1);
                user = rs.getString(2);
                postgres = rs.getString(3);
                timezone = rs.getString(4);
            }
            try (ResultSet rs = statement.executeQuery("select postgis_full_version()")) {
                rs.next();
                postgis = rs.getString(1);
            }
            if (!"postgres".equals(database)) {
                throw new IllegalStateException("DATABASE_NAME_CONFLICT: " + database);
            }
            Map<String, Integer> tableCounts = verifySchemasAndTables(statement);
            log.info("PostgreSQL数据库连接成功，database={}，user={}", database, user);
            log.info("PostgreSQL版本检查通过，version={}", postgres);
            log.info("PostGIS检查通过，version={}", postgis);
            log.info("MYTransitGPS数据库基础结构检查完成，coreTables={}，jbTables={}，kuchingTables={}，klTables={}，melakaTables={}",
                    tableCounts.get("core"), tableCounts.get("jb"), tableCounts.get("kuching"),
                    tableCounts.get("kl"), tableCounts.get("melaka"));
            return new DatabaseInfo(database, user, postgres, postgis, timezone, tableCounts);
        } catch (Exception ex) {
            log.error("PostgreSQL或PostGIS连接检查失败，exceptionClass={}，message={}",
                    ex.getClass().getSimpleName(), ex.getMessage(), ex);
            throw new IllegalStateException("PostgreSQL connection validation failed.", ex);
        }
    }

    private Map<String, Integer> verifySchemasAndTables(Statement statement) throws Exception {
        Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put("core", 4);
        expected.put("jb", 12);
        expected.put("kuching", 12);
        expected.put("kl", 12);
        expected.put("melaka", 12);
        Map<String, Integer> actual = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : expected.entrySet()) {
            String schema = entry.getKey();
            if (!schemaExists(statement, schema)) {
                throw new IllegalStateException("DATABASE_SCHEMA_MISSING: " + schema);
            }
            int count = tableCount(statement, schema);
            actual.put(schema, count);
            if (count != entry.getValue()) {
                throw new IllegalStateException("DATABASE_TABLE_COUNT_CONFLICT: " + schema
                        + " expected=" + entry.getValue() + " actual=" + count);
            }
        }
        return actual;
    }

    private boolean schemaExists(Statement statement, String schema) throws Exception {
        try (ResultSet rs = statement.executeQuery("select exists (select 1 from information_schema.schemata where schema_name = '" + schema + "')")) {
            rs.next();
            return rs.getBoolean(1);
        }
    }

    private int tableCount(Statement statement, String schema) throws Exception {
        try (ResultSet rs = statement.executeQuery("select count(*) from information_schema.tables where table_schema = '" + schema + "' and table_type = 'BASE TABLE'")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    /** PostgreSQL/PostGIS 连接与各业务 Schema 表数量的只读信息。 */
    public record DatabaseInfo(String database, String user, String postgresVersion, String postgisVersion,
                               String timezone, Map<String, Integer> tableCounts) {
    }
}
