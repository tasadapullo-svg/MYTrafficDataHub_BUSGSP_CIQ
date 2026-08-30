package com.mytransitgps.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 可选的 Redis 启动连通性检查器；仅在配置启用时执行一次探测。
 */
@Component
public class RedisStartupConnectionChecker implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RedisStartupConnectionChecker.class);
    private final AppRedisProperties redisProperties;
    private final StringRedisTemplate stringRedisTemplate;

    public RedisStartupConnectionChecker(
            AppRedisProperties redisProperties,
            StringRedisTemplate stringRedisTemplate
    ) {
        this.redisProperties = redisProperties;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!redisProperties.connectOnStartup()) {
            log.info("Redis启动连通性检查未启用，跳过启动Ping");
            return;
        }
        log.info("开始Redis启动连通性检查");
        try {
            stringRedisTemplate.getConnectionFactory().getConnection().ping();
            log.info("Redis启动连通性检查通过");
        } catch (RuntimeException ex) {
            log.error("Redis启动连通性检查失败，异常类型={}，错误信息={}",
                    ex.getClass().getSimpleName(), ex.getMessage());
            throw ex;
        }
    }
}
