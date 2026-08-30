package com.mytransitgps;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mytransitgps.modules.ciq.archive.CiqDailyArchiveJob;
import com.mytransitgps.modules.ciq.monitoring.CiqMonitoringRepository;
import com.mytransitgps.modules.ciq.persistence.CiqTrafficSpeedPersistenceService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 验证 CIQ archive 定时任务由 CIQ 总开关和 archive 子开关共同控制。
 */
class CiqArchiveConditionContextTest {
    @TempDir
    Path workspace;

    @Test
    void archiveJobIsAbsentWhenCiqDisabledEvenIfArchiveEnabled() {
        try (ConfigurableApplicationContext context = context("false", "true")) {
            assertFalse(context.containsBean("ciqDailyArchiveJob"));
            assertTrue(context.getBeanProvider(ObjectMapper.class).getIfAvailable() != null);
            assertFalse(context.getBeanProvider(CiqMonitoringRepository.class).iterator().hasNext());
            assertFalse(context.getBeanProvider(CiqTrafficSpeedPersistenceService.class).iterator().hasNext());
        }
    }

    @Test
    void archiveJobIsAbsentWhenArchiveDisabled() {
        try (ConfigurableApplicationContext context = context("true", "false")) {
            assertFalse(context.containsBean("ciqDailyArchiveJob"));
        }
    }

    @Test
    void archiveJobExistsWhenBothSwitchesEnabled() {
        try (ConfigurableApplicationContext context = context("true", "true")) {
            assertTrue(context.getBeanProvider(CiqDailyArchiveJob.class).getIfAvailable() != null);
        }
    }

    private ConfigurableApplicationContext context(String ciqEnabled, String archiveEnabled) {
        prepareWorkspace();
        return new SpringApplicationBuilder(MyTrafficDataHubApplication.class)
                .web(WebApplicationType.NONE)
                .properties(
                        "spring.autoconfigure.exclude="
                                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                                + "org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration,"
                                + "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration")
                .run(
                        "--spring.profiles.active=test",
                        "--traffic.ciq.enabled=" + ciqEnabled,
                        "--traffic.ciq.archive.enabled=" + archiveEnabled,
                        "--traffic.ciq.schedule.enabled=false",
                        "--traffic.ciq.collectors.traffic-speed.enabled=false",
                        "--traffic.ciq.persistence.database-write-enabled=false",
                        "--mytransitgps.database.enabled=false",
                        "--mytransitgps.workspace-root=" + workspace,
                        "--mytransitgps.continuous.enabled=false",
                        "--archive.daily.enabled=false");
    }

    private void prepareWorkspace() {
        try {
            Files.createDirectories(workspace.resolve("BusGPS"));
            Files.createDirectories(workspace.resolve("CIQ"));
            Files.createDirectories(workspace.resolve("logs"));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
