package com.mytransitgps;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 验证 Application 模块最小 Spring Context 可启动。
 */
@SpringBootTest(properties = {
        "traffic.ciq.enabled=false",
        "mytransitgps.database.enabled=false",
        "mytransitgps.continuous.enabled=false",
        "archive.daily.enabled=false"
})
@ActiveProfiles("test")
class MyTrafficDataHubApplicationTests {
    @Autowired
    ObjectMapper objectMapper;

    @Test
    void contextLoadsWithDatabaseAndCiqDisabled() {
        assertNotNull(objectMapper);
    }
}

