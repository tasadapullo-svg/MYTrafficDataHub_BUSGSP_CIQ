package com.mytransitgps.modules.ciqbus.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mytransitgps.modules.ciqbus.client.LtaCrossBorderBusClient;
import com.mytransitgps.modules.ciqbus.parser.CiqBusRouteStopResolver;
import com.mytransitgps.modules.ciqbus.parser.LtaBusArrivalParser;
import com.mytransitgps.modules.ciqbus.persistence.CiqBusRealtimeRepository;
import com.mytransitgps.modules.ciqbus.redis.CiqBusEventStateStore;
import com.mytransitgps.modules.ciqbus.redis.CiqBusRedisEventStateStore;
import com.mytransitgps.modules.ciqbus.service.CiqBusCollectionScheduler;
import com.mytransitgps.modules.ciqbus.service.CiqBusDailyExportScheduler;
import com.mytransitgps.modules.ciqbus.service.CiqBusDailyExportService;
import com.mytransitgps.modules.ciqbus.service.CiqBusEventMatcher;
import com.mytransitgps.modules.ciqbus.service.CiqBusRealtimeService;
import com.mytransitgps.platform.http.TrafficHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods = false)
public class CiqBusModuleConfiguration {
    @Bean
    LtaCrossBorderBusClient ltaCrossBorderBusClient(TrafficHttpClient httpClient, CiqBusProperties properties) {
        return new LtaCrossBorderBusClient(httpClient, properties);
    }

    @Bean
    CiqBusRouteStopResolver ciqBusRouteStopResolver(CiqBusProperties properties) {
        return new CiqBusRouteStopResolver(properties);
    }

    @Bean
    LtaBusArrivalParser ltaBusArrivalParser(ObjectMapper objectMapper, CiqBusProperties properties,
                                            CiqBusRouteStopResolver resolver) {
        return new LtaBusArrivalParser(objectMapper, properties, resolver);
    }

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    CiqBusEventStateStore ciqBusEventStateStore(StringRedisTemplate redis, ObjectMapper objectMapper) {
        return new CiqBusRedisEventStateStore(redis, objectMapper);
    }

    @Bean
    @ConditionalOnBean(JdbcTemplate.class)
    CiqBusRealtimeRepository ciqBusRealtimeRepository(JdbcTemplate jdbcTemplate) {
        return new CiqBusRealtimeRepository(jdbcTemplate);
    }

    @Bean
    @ConditionalOnBean(CiqBusEventStateStore.class)
    CiqBusEventMatcher ciqBusEventMatcher(CiqBusProperties properties, CiqBusEventStateStore stateStore) {
        return new CiqBusEventMatcher(properties, stateStore);
    }

    @Bean
    @ConditionalOnBean({CiqBusEventMatcher.class, CiqBusRealtimeRepository.class})
    CiqBusRealtimeService ciqBusRealtimeService(LtaCrossBorderBusClient ltaClient,
                                                LtaBusArrivalParser parser,
                                                CiqBusEventMatcher matcher,
                                                CiqBusEventStateStore stateStore,
                                                CiqBusRealtimeRepository repository,
                                                CiqBusRouteStopResolver stopResolver,
                                                CiqBusProperties properties) {
        return new CiqBusRealtimeService(ltaClient, parser, matcher, stateStore, repository, stopResolver, properties);
    }

    @Bean
    @ConditionalOnBean(CiqBusRealtimeRepository.class)
    CiqBusDailyExportService ciqBusDailyExportService(CiqBusRealtimeRepository repository, CiqBusProperties properties) {
        return new CiqBusDailyExportService(repository, properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "ciqbus.scheduler", name = "enabled", havingValue = "true")
    @ConditionalOnBean(CiqBusRealtimeService.class)
    CiqBusCollectionScheduler ciqBusCollectionScheduler(CiqBusRealtimeService service) {
        return new CiqBusCollectionScheduler(service);
    }

    @Bean
    @ConditionalOnBean(CiqBusDailyExportService.class)
    CiqBusDailyExportScheduler ciqBusDailyExportScheduler(CiqBusDailyExportService service) {
        return new CiqBusDailyExportScheduler(service);
    }
}
