package com.mytransitgps.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.redis")
/**
 * Redis 启动行为配置，控制应用启动时是否主动验证 Redis 连接。
 */
public record AppRedisProperties(boolean connectOnStartup) {
}
