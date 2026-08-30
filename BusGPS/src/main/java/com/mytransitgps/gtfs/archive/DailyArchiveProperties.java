package com.mytransitgps.gtfs.archive;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 每日 ZIP 归档的开关、执行时间和业务时区配置。
 */
@ConfigurationProperties(prefix = "archive.daily")
public class DailyArchiveProperties {

    private boolean enabled = false;
    private String cron = "0 30 0 * * *";
    private String zone = "Asia/Kuala_Lumpur";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }
}
