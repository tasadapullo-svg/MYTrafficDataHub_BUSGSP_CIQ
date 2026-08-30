package com.mytransitgps.dashboard.config;

import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 交通数据监测大屏 JDBC 只读查询配置。
 *
 * <p>
 * 直接复用 Spring Boot 主数据源。
 * Dashboard 仅执行只读查询，不参与 BUS GPS / CIQ 数据写入。
 * </p>
 *
 * <p>
 * 注意：
 * 不再使用 @ConditionalOnBean(DataSource.class)。
 * 在多模块工程中，ConditionalOnBean 依赖 Bean 注册顺序，
 * 可能导致 Dashboard JdbcTemplate 未被注册。
 * </p>
 */
@Configuration
@ConditionalOnProperty(
        prefix = "mytransitgps.database",
        name = "enabled",
        havingValue = "true"
)
public class DashboardDatabaseConfig {

    /**
     * Dashboard 专用只读 JdbcTemplate。
     */
    @Bean("dashboardJdbcTemplate")
    public JdbcTemplate dashboardJdbcTemplate(DataSource dataSource) {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        // 单次大屏查询最长 8 秒，防止慢 SQL 长时间阻塞。
        jdbc.setQueryTimeout(8);

        // 大批量轨迹读取时使用 FetchSize。
        jdbc.setFetchSize(1000);

        return jdbc;
    }
}