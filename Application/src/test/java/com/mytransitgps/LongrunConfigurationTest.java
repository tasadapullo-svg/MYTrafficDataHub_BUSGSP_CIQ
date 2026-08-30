package com.mytransitgps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mytransitgps.config.AppRedisProperties;
import com.mytransitgps.modules.ciq.config.CiqProperties;
import com.mytransitgps.persistence.config.MyTransitGpsDatabaseProperties;
import com.mytransitgps.platform.config.BusGpsProperties;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

/** 验证 IDEA longrun YAML 已包含用户要求的手工入库测试配置。 */
class LongrunConfigurationTest {

    @Test
    void longrunYamlMustMatchManualDatabaseTestSemantics() throws IOException {
        StandardEnvironment environment = new StandardEnvironment();
        MutablePropertySources sources = environment.getPropertySources();
        loadYaml(sources, "application-longrun.yml");
        loadYaml(sources, "config/ciq.yml");
        loadYaml(sources, "config/bus-gps.yml");
        loadYaml(sources, "application.yml");

        Binder binder = Binder.get(environment);
        CiqProperties ciq = binder.bind("traffic.ciq", CiqProperties.class)
                .orElseThrow(() -> new IllegalStateException("traffic.ciq 配置绑定失败"));
        BusGpsProperties busGps = binder.bind("traffic.bus-gps", BusGpsProperties.class)
                .orElseThrow(() -> new IllegalStateException("traffic.bus-gps 配置绑定失败"));
        MyTransitGpsDatabaseProperties database =
                binder.bind("mytransitgps", MyTransitGpsDatabaseProperties.class)
                        .orElseThrow(() -> new IllegalStateException("mytransitgps 配置绑定失败"));
        AppRedisProperties redis = binder.bind("app.redis", AppRedisProperties.class)
                .orElseThrow(() -> new IllegalStateException("app.redis 配置绑定失败"));

        assertTrue(busGps.isEnabled());
        assertEquals(5, busGps.getFeeds().size());
        assertTrue(database.getDatabase().isEnabled());
        assertTrue(database.getContinuous().isEnabled());
        assertEquals("data_download", database.getWorkspaceRoot());
        assertEquals("jdbc:postgresql://localhost:5432/postgres", environment.getProperty("spring.datasource.url"));
        assertEquals("postgres", environment.getProperty("spring.datasource.username"));
        assertEquals("123456", environment.getProperty("spring.datasource.password"));
        assertEquals("C:/Users/DELL/Desktop/MYTrafficDataHub_20260829/data_download/logs", environment.getProperty("traffic.file-log-root"));
        assertTrue(ciq.isEnabled());
        assertTrue(ciq.getCollectors().getTrafficSpeed().isEnabled());
        assertTrue(ciq.getCollectors().getEstimatedTravelTimes().isEnabled());
        assertTrue(ciq.getCollectors().getTrafficIncidents().isEnabled());
        assertTrue(ciq.getCollectors().getVms().isEnabled());
        assertTrue(ciq.getCollectors().getFaultyTrafficLights().isEnabled());
        assertTrue(ciq.getCollectors().getRoadWorks().isEnabled());
        assertTrue(ciq.getCollectors().getTrafficFlow().isEnabled());
        assertTrue(ciq.getCollectors().getRoadOpenings().isEnabled());
        assertTrue(ciq.getPersistence().isDatabaseWriteEnabled());
        assertTrue(ciq.getSchedule().isEnabled());
        assertTrue(ciq.getArchive().isEnabled());
        assertTrue(ciq.getManualTest().isEnabled());
        assertEquals(500, ciq.getPersistence().getBatchSize());
        assertEquals("0 0,15,30,45 * * * *", ciq.getSchedule().getCron());
        assertEquals("Asia/Kuala_Lumpur", ciq.getTimezone());
        assertEquals("https://datamall2.mytransport.sg/ltaodataservice", ciq.getLta().getBaseUrl());
        assertTrue(ciq.isAccountKeyConfigured());
        assertFalse(redis.connectOnStartup());
    }

    private static void loadYaml(MutablePropertySources sources, String classpathLocation) throws IOException {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        for (var source : loader.load(classpathLocation, new ClassPathResource(classpathLocation))) {
            sources.addLast(source);
        }
    }
}
