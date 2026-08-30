package com.mytransitgps.persistence.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mytransitgps.persistence.routing.CitySchemaSqlInterceptor;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 仅在明确启用数据库写入时装配 MYTransitGPS 的 MyBatis 基础设施。
 */
@Configuration
@ConditionalOnProperty(prefix = "mytransitgps.database", name = "enabled", havingValue = "true")
public class MyBatisDatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(MyBatisDatabaseConfig.class);

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    ObjectMapper persistenceObjectMapper() {
        return new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Bean
    CitySchemaSqlInterceptor citySchemaSqlInterceptor() {
        log.info("MYTransitGPS MyBatis 数据库配置已初始化，城市 Schema 路由已启用");
        return new CitySchemaSqlInterceptor();
    }

    @Bean
    static MapperScannerConfigurer myTransitGpsMapperScannerConfigurer() {
        MapperScannerConfigurer scanner = new MapperScannerConfigurer();
        scanner.setBasePackage("com.mytransitgps.persistence.mapper");
        return scanner;
    }
}
