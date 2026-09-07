package com.mytransitgps.modules.ciqbus.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mytransitgps.modules.ciqbus.domain.CiqBusActiveEvent;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;

public class CiqBusRedisEventStateStore implements CiqBusEventStateStore {
    private static final Logger log = LoggerFactory.getLogger(CiqBusRedisEventStateStore.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public CiqBusRedisEventStateStore(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<CiqBusActiveEvent> findActive(String routeNo, String directionCode) {
        String index = indexKey(routeNo, directionCode);
        log.info("[CIQBUS-REDIS] 项目进度：读取Redis活跃事件索引，indexKey={}", index);
        Set<String> ids = redis.opsForSet().members(index);
        if (ids == null || ids.isEmpty()) {
            log.info("[CIQBUS-REDIS] 项目进度：Redis活跃事件索引为空，routeNo={}，directionCode={}", routeNo, directionCode);
            return List.of();
        }
        List<CiqBusActiveEvent> events = new ArrayList<>();
        for (String id : ids) {
            String json = redis.opsForValue().get(activeKey(routeNo, directionCode, id));
            if (json != null) {
                try {
                    events.add(objectMapper.readValue(json, CiqBusActiveEvent.class));
                } catch (JsonProcessingException ex) {
                    throw new RedisSystemException("Invalid CIQBus active event JSON", ex);
                }
            }
        }
        log.info("[CIQBUS-REDIS] 项目进度：Redis活跃事件读取完成，indexKey={}，事件数={}", index, events.size());
        return events;
    }

    @Override
    public void saveActive(CiqBusActiveEvent event, Duration ttl) {
        try {
            String key = activeKey(event.getRouteNo(), event.getDirectionCode(), event.getMatchedEventId());
            redis.opsForValue().set(key, objectMapper.writeValueAsString(event), ttl);
            String index = indexKey(event.getRouteNo(), event.getDirectionCode());
            redis.opsForSet().add(index, event.getMatchedEventId());
            redis.expire(index, ttl);
            log.info("[CIQBUS-REDIS] 项目进度：已保存CIQBus活跃事件到Redis，activeKey={}，indexKey={}，ttlSeconds={}",
                    key, index, ttl.toSeconds());
        } catch (JsonProcessingException ex) {
            throw new RedisSystemException("CIQBus active event JSON serialize failed", ex);
        }
    }

    @Override
    public boolean markPassageIfAbsent(String matchedEventId, String stopCode, Duration ttl) {
        String key = passageKey(matchedEventId, stopCode);
        Boolean ok = redis.opsForValue().setIfAbsent(key, "1", ttl);
        boolean inserted = Boolean.TRUE.equals(ok);
        log.info("[CIQBUS-REDIS] 项目进度：写入过站去重Key，passageKey={}，首次写入={}，ttlSeconds={}",
                key, inserted, ttl.toSeconds());
        return inserted;
    }

    @Override
    public void markCompleted(String fingerprint, Duration ttl) {
        String key = "ciqbus:completed:" + fingerprint;
        redis.opsForValue().set(key, "1", ttl);
        log.info("[CIQBUS-REDIS] 项目进度：已标记完整过境事件完成，completedKey={}，ttlSeconds={}", key, ttl.toSeconds());
    }

    @Override
    public Long ttlSeconds(String key) {
        return redis.getExpire(key);
    }

    public static String activeKey(String routeNo, String directionCode, String matchedEventId) {
        return "ciqbus:active:" + routeNo + ":" + directionCode + ":" + matchedEventId;
    }

    public static String indexKey(String routeNo, String directionCode) {
        return "ciqbus:index:" + routeNo + ":" + directionCode;
    }

    public static String passageKey(String matchedEventId, String stopCode) {
        return "ciqbus:passage:" + matchedEventId + ":" + stopCode;
    }
}
