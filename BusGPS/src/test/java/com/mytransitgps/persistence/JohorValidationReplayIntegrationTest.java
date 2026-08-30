package com.mytransitgps.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Path;

import com.mytransitgps.persistence.service.JohorValidationReplayService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** 显式只读重放已完成五轮采集的磁盘 JSON ↔ PostgreSQL 验证，不产生任何网络请求。 */
@Tag("db-validation-replay")
@SpringBootTest(properties = {"mytransitgps.database.enabled=true", "mytransitgps.db-test.enabled=false", "app.redis.connect-on-startup=false"})
class JohorValidationReplayIntegrationTest {
    @Autowired JohorValidationReplayService replay;
    @Test void validatesExistingRunWithoutNetwork() {
        assumeTrue(Boolean.getBoolean("mytransitgps.db-validation-replay.confirm"));
        String runCode = System.getProperty("mytransitgps.db-validation-run-code");
        var result = replay.replay(Path.of(System.getProperty("user.dir")).toAbsolutePath().getParent(), runCode);
        assertThat(result.fieldMismatches()).isZero(); assertThat(result.passed()).isTrue();
    }
}
