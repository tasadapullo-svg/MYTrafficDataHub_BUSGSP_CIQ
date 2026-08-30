package com.mytransitgps.gtfs.config;

import java.util.List;
import java.util.Optional;
import com.mytransitgps.platform.config.BusGpsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GTFS Feed 注册表，提供受控 Feed 清单和按 ID 查询能力。
 */
public class GtfsFeedRegistry {

    private static final Logger log = LoggerFactory.getLogger(GtfsFeedRegistry.class);
    private static final List<GtfsFeedDefinition> LEGACY_FEEDS = List.of(
            new GtfsFeedDefinition(
                    "mybas-johor",
                    "johor_bahru",
                    "Johor_Bahru",
                    "Johor Bahru",
                    "BAS.MY",
                    "bus",
                    "https://api.data.gov.my/gtfs-realtime/vehicle-position/mybas-johor/",
                    "https://api.data.gov.my/gtfs-static/mybas-johor",
                    "johor_bahru",
                    true),
            new GtfsFeedDefinition(
                    "mybas-kuching",
                    "kuching",
                    "Kuching",
                    "Kuching",
                    "BAS.MY",
                    "bus",
                    "https://api.data.gov.my/gtfs-realtime/vehicle-position/mybas-kuching/",
                    "https://api.data.gov.my/gtfs-static/mybas-kuching",
                    "kuching",
                    true),
            new GtfsFeedDefinition(
                    "rapid-bus-kl",
                    "kuala_lumpur",
                    "Kuala_Lumpur",
                    "Kuala Lumpur",
                    "Prasarana",
                    "rapid_bus",
                    "https://api.data.gov.my/gtfs-realtime/vehicle-position/prasarana?category=rapid-bus-kl",
                    "https://api.data.gov.my/gtfs-static/prasarana?category=rapid-bus-kl",
                    "kuala_lumpur_rapid_bus",
                    true),
            new GtfsFeedDefinition(
                    "rapid-bus-mrtfeeder",
                    "kuala_lumpur",
                    "Kuala_Lumpur",
                    "Kuala Lumpur",
                    "Prasarana",
                    "mrt_feeder",
                    "https://api.data.gov.my/gtfs-realtime/vehicle-position/prasarana?category=rapid-bus-mrtfeeder",
                    "https://api.data.gov.my/gtfs-static/prasarana?category=rapid-bus-mrtfeeder",
                    "kuala_lumpur_mrt_feeder",
                    true),
            new GtfsFeedDefinition(
                    "mybas-melaka",
                    "melaka",
                    "Melaka",
                    "Melaka",
                    "BAS.MY",
                    "bus",
                    "https://api.data.gov.my/gtfs-realtime/vehicle-position/mybas-melaka/",
                    "https://api.data.gov.my/gtfs-static/mybas-melaka",
                    "melaka",
                    true));

    private final List<GtfsFeedDefinition> feeds;

    /** 保留原构造方式，使稳定BUS GPS调用链完全沿用既有五路默认值。 */
    public GtfsFeedRegistry() {
        this.feeds = LEGACY_FEEDS;
        logInitialization("legacy");
    }

    /** 新平台配置入口；第一阶段仅建立兼容迁移接缝。 */
    public GtfsFeedRegistry(BusGpsProperties properties) {
        if (properties == null || properties.getFeeds().isEmpty()) {
            this.feeds = LEGACY_FEEDS;
            log.warn("BUS GPS配置未提供Feed，继续使用稳定默认清单");
        } else {
            this.feeds = properties.getFeeds().values().stream()
                    .map(GtfsFeedRegistry::toDefinition)
                    .toList();
        }
        logInitialization("configuration");
    }

    private void logInitialization(String source) {
        log.info("GTFS Feed注册表初始化完成，feedCount={}，enabledFeedCount={}",
                feeds.size(), feeds.stream().filter(GtfsFeedDefinition::enabled).count());
        log.debug("GTFS Feed注册表来源={}", source);
    }

    private static GtfsFeedDefinition toDefinition(BusGpsProperties.Feed feed) {
        return new GtfsFeedDefinition(
                feed.getFeedId(), feed.getCityCode(), feed.getCityFolder(), feed.getCityName(),
                feed.getProvider(), feed.getMode(), feed.getRealtimeUrl(), feed.getStaticUrl(),
                feed.getSchemaKey(), feed.isEnabled());
    }

    public List<GtfsFeedDefinition> getAllFeeds() {
        return feeds;
    }

    public List<GtfsFeedDefinition> getEnabledFeeds() {
        return feeds.stream().filter(GtfsFeedDefinition::enabled).toList();
    }

    public Optional<GtfsFeedDefinition> findByFeedId(String feedId) {
        return feeds.stream().filter(feed -> feed.feedId().equals(feedId)).findFirst();
    }
}
