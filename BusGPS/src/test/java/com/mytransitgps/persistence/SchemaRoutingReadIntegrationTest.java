package com.mytransitgps.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.mytransitgps.persistence.mapper.JbStaticVersionMapper;
import com.mytransitgps.persistence.routing.CitySchemaRouter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** 显式数据库只读集成测试：验证同一 Mapper 可安全路由四个城市 Schema。 */
@Tag("multicity-db-read")
@SpringBootTest(properties = {"mytransitgps.database.enabled=true", "app.redis.connect-on-startup=false"})
class SchemaRoutingReadIntegrationTest {
    @Autowired CitySchemaRouter router;
    @Autowired JbStaticVersionMapper mapper;

    @Test void routesOneMapperAcrossAllSchemasWithoutSearchPathChanges() {
        assertThat(router.withSchema("jb", () -> mapper.selectCount(null))).isGreaterThanOrEqualTo(0);
        assertThat(router.withSchema("kuching", () -> mapper.selectCount(null))).isGreaterThanOrEqualTo(0);
        assertThat(router.withSchema("kl", () -> mapper.selectCount(null))).isGreaterThanOrEqualTo(0);
        assertThat(router.withSchema("melaka", () -> mapper.selectCount(null))).isGreaterThanOrEqualTo(0);
    }
}
