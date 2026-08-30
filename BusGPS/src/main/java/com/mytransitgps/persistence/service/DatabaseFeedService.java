package com.mytransitgps.persistence.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mytransitgps.persistence.entity.CoreGtfsFeedEntity;
import com.mytransitgps.persistence.mapper.CoreGtfsFeedMapper;
import com.mytransitgps.persistence.entity.CoreStudyCityEntity;
import com.mytransitgps.persistence.mapper.CoreStudyCityMapper;
import com.mytransitgps.persistence.model.DatabaseFeedContext;
import com.mytransitgps.persistence.routing.CitySchemaRouter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 中文名称：数据库 GTFS Feed 配置读取服务。
 *
 * 功能说明：从 core.gtfs_feed 获取真实 feed_uid 和冻结后的采集参数，
 * 不从 Java 常量推测或硬编码数据库 UUID。
 */
@Service
@ConditionalOnProperty(prefix = "mytransitgps.database", name = "enabled", havingValue = "true")
public class DatabaseFeedService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseFeedService.class);
    private final CoreGtfsFeedMapper mapper;
    private final CoreStudyCityMapper cityMapper;
    private final CitySchemaRouter schemaRouter;

    public DatabaseFeedService(CoreGtfsFeedMapper mapper, CoreStudyCityMapper cityMapper, CitySchemaRouter schemaRouter) {
        this.mapper = mapper;
        this.cityMapper = cityMapper;
        this.schemaRouter = schemaRouter;
    }

    public CoreGtfsFeedEntity requireFeed(String feedId) {
        log.info("开始读取GTFS Feed配置，feedId={}", feedId);
        CoreGtfsFeedEntity feed = mapper.selectOne(new QueryWrapper<CoreGtfsFeedEntity>().eq("feed_id", feedId));
        if (feed == null) {
            log.error("数据库Feed配置缺失，feedId={}", feedId);
            throw new IllegalStateException("Database feed not found: " + feedId);
        }
        log.info("获取{} feed_uid完成，feedUid={}", feedId, feed.uid);
        return feed;
    }

    public DatabaseFeedContext requireContext(String feedId) {
        CoreGtfsFeedEntity feed = requireFeed(feedId);
        CoreStudyCityEntity city = cityMapper.selectById(feed.cityUid);
        if (city == null) {
            log.error("数据库城市配置缺失，feedId={}，cityUid={}", feedId, feed.cityUid);
            throw new IllegalStateException("Database city not found for feed: " + feedId);
        }
        String schema = schemaRouter.requireAllowed(city.schemaName);
        if (!Boolean.TRUE.equals(feed.enabled) || !Boolean.TRUE.equals(city.enabled)) {
            log.error("数据库Feed或城市未启用，feedId={}，feedEnabled={}，cityEnabled={}",
                    feedId, feed.enabled, city.enabled);
            throw new IllegalStateException("Feed or city is disabled: " + feedId);
        }
        if (feed.pollIntervalSeconds == null || feed.staggerOffsetSeconds == null) {
            log.error("数据库Feed调度字段缺失，feedId={}，pollIntervalSeconds={}，staggerOffsetSeconds={}",
                    feedId, feed.pollIntervalSeconds, feed.staggerOffsetSeconds);
            throw new IllegalStateException("Missing database scheduling fields for feed: " + feedId);
        }
        DatabaseFeedContext context = new DatabaseFeedContext(feed.uid, feed.feedId, feed.cityUid,
                city.cityCode, city.cityName, schema, feed.realtimeUrl, feed.staticUrl, feed.filePrefix,
                feed.pollIntervalSeconds, feed.staggerOffsetSeconds);
        log.info("Feed数据库配置加载完成，城市={}，feedId={}，feedUid={}，schema={}，enabled={}，pollIntervalSeconds={}，staggerOffsetSeconds={}",
                city.cityName, feed.feedId, feed.uid, schema, feed.enabled,
                feed.pollIntervalSeconds, feed.staggerOffsetSeconds);
        return context;
    }

    public List<DatabaseFeedContext> requireContexts(List<String> feedIds) {
        log.info("开始从core.gtfs_feed加载多城市Feed配置，requestedFeedCount={}", feedIds.size());
        List<DatabaseFeedContext> contexts = feedIds.stream().map(this::requireContext).toList();
        log.info("多城市Feed配置加载完成，feedCount={}", contexts.size());
        return contexts;
    }
}
