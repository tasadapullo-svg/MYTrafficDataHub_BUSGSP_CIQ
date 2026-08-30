package com.mytransitgps.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** 验证长期运行日志按级别分文件、使用统一日志根目录并具有磁盘容量上限。 */
class LogConfigurationTest {
    @Test
    void definesExactLevelFilesRollingAndCapacityCaps() throws Exception {
        String xml = Files.readString(Path.of("../Application/src/main/resources/logback-spring.xml"));
        assertThat(xml).contains("traffic.file-log-root", "data_download/logs", "LevelFilter",
                "<level>INFO</level>", "<level>WARN</level>", "<level>ERROR</level>",
                "<maxFileSize>200MB</maxFileSize>", "<totalSizeCap>15GB</totalSizeCap>",
                "<totalSizeCap>5GB</totalSizeCap>");
        assertThat(xml).contains("${LOG_ROOT}/info", "${LOG_ROOT}/warn", "${LOG_ROOT}/error");
        assertThat(xml).doesNotContain("<maxHistory>3650</maxHistory>", "<totalSizeCap>0</totalSizeCap>");
    }
}
