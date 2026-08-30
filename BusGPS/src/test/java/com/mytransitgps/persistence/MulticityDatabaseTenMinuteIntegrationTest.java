package com.mytransitgps.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.mytransitgps.persistence.model.MulticityRunResult;
import com.mytransitgps.persistence.service.MulticityDatabaseTenMinuteRunner;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** 仅供显式执行的四城市五 Feed、五轮、120 秒真实数据库验收测试。 */
@Tag("multicity-db-10min")
@SpringBootTest(properties = {"mytransitgps.database.enabled=true", "mytransitgps.multicity-db-test.enabled=true", "app.redis.connect-on-startup=false"})
class MulticityDatabaseTenMinuteIntegrationTest {
    @Autowired MulticityDatabaseTenMinuteRunner runner;

    @Test void runsTwentyFiveRealAttemptsFromDiskJsonIntoCitySchemas() {
        assumeTrue(Boolean.getBoolean("mytransitgps.multicity-db-test.confirm"),
                "需要 -Dmytransitgps.multicity-db-test.confirm=true 显式确认");
        Path workspace = Path.of(System.getProperty("user.dir")).toAbsolutePath().getParent();
        MulticityRunResult result = runner.run(workspace);
        assertThat(result.attempts()).hasSize(25);
        assertThat(result.attempts()).allMatch(attempt -> attempt.persistenceResult() != null);
        assertThat(result.passed()).isTrue();
    }
}
