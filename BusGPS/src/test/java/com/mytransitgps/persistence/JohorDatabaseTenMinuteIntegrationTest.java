package com.mytransitgps.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Path;

import com.mytransitgps.persistence.model.JohorRunResult;
import com.mytransitgps.persistence.service.JohorDatabaseTenMinuteRunner;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** 仅供显式执行的 Johor Bahru 五轮、120秒间隔真实数据库验收测试。 */
@Tag("db-10min")
@SpringBootTest(properties = {"mytransitgps.database.enabled=true", "mytransitgps.db-test.enabled=true", "app.redis.connect-on-startup=false"})
class JohorDatabaseTenMinuteIntegrationTest {
    @Autowired JohorDatabaseTenMinuteRunner runner;

    @Test void runsFiveRealJohorAttemptsAndValidatesDiskJsonAgainstPostgresql() {
        assumeTrue(Boolean.getBoolean("mytransitgps.db-test.confirm"), "需要 -Dmytransitgps.db-test.confirm=true 显式确认");
        Path project = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path workspace = project.getParent();
        JohorRunResult result = runner.run(workspace);
        assertThat(result.cycles()).hasSize(5); assertThat(result.fieldMismatchCount()).isZero(); assertThat(result.passed()).isTrue();
    }
}
