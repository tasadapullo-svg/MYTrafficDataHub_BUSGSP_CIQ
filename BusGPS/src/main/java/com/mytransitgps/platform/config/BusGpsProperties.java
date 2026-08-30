package com.mytransitgps.platform.config;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** BUS GPS 模块配置；保留 Feed 顺序以维持现有五路采集顺序。 */
@ConfigurationProperties(prefix = "traffic.bus-gps")
public class BusGpsProperties {

    private boolean enabled = true;
    private Map<String, Feed> feeds = new LinkedHashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, Feed> getFeeds() {
        return feeds;
    }

    public void setFeeds(Map<String, Feed> feeds) {
        this.feeds = feeds == null ? new LinkedHashMap<>() : new LinkedHashMap<>(feeds);
    }

    /** 单个 BUS GPS Feed 的启用状态、URL、城市与 Schema 配置。 */
    public static class Feed {
        private boolean enabled = true;
        private String feedId;
        private String cityCode;
        private String cityFolder;
        private String cityName;
        private String provider;
        private String mode;
        private String realtimeUrl;
        private String staticUrl;
        private String schemaKey;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getFeedId() { return feedId; }
        public void setFeedId(String feedId) { this.feedId = feedId; }
        public String getCityCode() { return cityCode; }
        public void setCityCode(String cityCode) { this.cityCode = cityCode; }
        public String getCityFolder() { return cityFolder; }
        public void setCityFolder(String cityFolder) { this.cityFolder = cityFolder; }
        public String getCityName() { return cityName; }
        public void setCityName(String cityName) { this.cityName = cityName; }
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
        public String getRealtimeUrl() { return realtimeUrl; }
        public void setRealtimeUrl(String realtimeUrl) { this.realtimeUrl = realtimeUrl; }
        public String getStaticUrl() { return staticUrl; }
        public void setStaticUrl(String staticUrl) { this.staticUrl = staticUrl; }
        public String getSchemaKey() { return schemaKey; }
        public void setSchemaKey(String schemaKey) { this.schemaKey = schemaKey; }
    }
}
