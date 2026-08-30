package com.mytransitgps.modules.ciq;

import static org.mockito.Mockito.mock;

import com.mytransitgps.modules.ciq.config.CiqProperties;
import com.mytransitgps.modules.ciq.monitoring.CiqMonitoringRepository;
import com.mytransitgps.modules.ciq.persistence.CiqTrafficSpeedPersistenceService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 验证 CIQ JDBC Bean 在 JdbcTemplate 已存在时按配置稳定装配。
 *
 * <p>不再使用 @ConditionalOnBean(JdbcTemplate.class)，避免多模块 Bean 注册顺序导致 API01 Persistence
 * 偶发缺失。没有 JdbcTemplate 的数据库启用场景应由 Spring 直接 fail-fast，而不是静默丢失 Bean。</p>
 */
class CiqJdbcConditionalBeansTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(CiqProperties.class)
            .withBean(JdbcTemplate.class, () -> mock(JdbcTemplate.class))
            .withUserConfiguration(CiqTrafficSpeedPersistenceService.class, CiqMonitoringRepository.class);

    @Test
    void rawOnlyCreatesMonitoringButNotPersistence() {
        runner.withPropertyValues(
                        "traffic.ciq.enabled=true",
                        "traffic.ciq.persistence.database-write-enabled=false")
                .run(context -> {
                    context.assertThat().hasSingleBean(CiqMonitoringRepository.class);
                    context.assertThat().doesNotHaveBean(CiqTrafficSpeedPersistenceService.class);
                });
    }

    @Test
    void enabledDatabaseWriteCreatesBothJdbcBeans() {
        runner.withPropertyValues(
                        "traffic.ciq.enabled=true",
                        "traffic.ciq.persistence.database-write-enabled=true")
                .run(context -> {
                    context.assertThat().hasSingleBean(CiqMonitoringRepository.class);
                    context.assertThat().hasSingleBean(CiqTrafficSpeedPersistenceService.class);
                });
    }
}
