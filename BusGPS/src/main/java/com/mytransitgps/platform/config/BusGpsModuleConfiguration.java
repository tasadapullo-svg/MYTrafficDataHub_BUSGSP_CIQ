package com.mytransitgps.platform.config;

import com.mytransitgps.gtfs.config.GtfsFeedRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 为后续迁移提供配置驱动的 Feed 注册表，不替换现有稳定调用点。 */
@Configuration(proxyBeanMethods = false)
public class BusGpsModuleConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "traffic.bus-gps", name = "enabled", havingValue = "true", matchIfMissing = true)
    GtfsFeedRegistry configuredGtfsFeedRegistry(BusGpsProperties properties) {
        return new GtfsFeedRegistry(properties);
    }
}
