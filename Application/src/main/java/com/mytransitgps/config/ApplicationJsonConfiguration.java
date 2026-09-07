package com.mytransitgps.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 应用级 JSON 配置。
 *
 * <p>ObjectMapper 生命周期独立于数据库、Redis、BUS GPS 和 CIQ 开关，保证归档、Dashboard 和
 * 只读工具在数据库关闭时仍能正常装配。</p>
 */
@Configuration(proxyBeanMethods = false)
public class ApplicationJsonConfiguration {
    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }
}
