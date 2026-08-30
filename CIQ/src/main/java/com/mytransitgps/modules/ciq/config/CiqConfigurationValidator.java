package com.mytransitgps.modules.ciq.config;

import java.net.URI;
import org.springframework.stereotype.Component;

/**
 * CIQ 配置状态校验器。
 *
 * <p>缺少密钥或业务配置时只返回可监测状态，不终止 Spring Boot；数据库写入关闭时仍允许 Raw 采集。</p>
 */
@Component
public class CiqConfigurationValidator {

    public CiqConfigurationStatus status(CiqProperties properties) {
        if (!properties.isEnabled()) {
            return CiqConfigurationStatus.DISABLED;
        }
        if (blank(properties.getLta().getBaseUrl())
                || blank(properties.getLta().getAccountKey())
                || blank(properties.getTimezone())
                || blank(properties.getStorage().getRootDirectory())) {
            return CiqConfigurationStatus.NOT_CONFIGURED;
        }
        try {
            if (!URI.create(properties.getLta().getBaseUrl()).isAbsolute()) {
                return CiqConfigurationStatus.NOT_CONFIGURED;
            }
        } catch (IllegalArgumentException ex) {
            return CiqConfigurationStatus.NOT_CONFIGURED;
        }

        CiqProperties.Collectors collectors = properties.getCollectors();
        boolean anyEnabled = false;
        anyEnabled |= configured(collectors.getTrafficSpeed());
        anyEnabled |= configured(collectors.getEstimatedTravelTimes());
        anyEnabled |= configured(collectors.getTrafficIncidents());
        anyEnabled |= configured(collectors.getVms());
        anyEnabled |= configured(collectors.getFaultyTrafficLights());
        anyEnabled |= configured(collectors.getRoadWorks());
        anyEnabled |= configured(collectors.getTrafficFlow());
        anyEnabled |= configured(collectors.getRoadOpenings());

        if (!anyCollectorEnabled(collectors)) {
            return CiqConfigurationStatus.COLLECTOR_DISABLED;
        }
        if (!anyEnabled) {
            return CiqConfigurationStatus.NOT_CONFIGURED;
        }
        if (hasEnabledCollectorWithMissingConfiguration(collectors)) {
            return CiqConfigurationStatus.NOT_CONFIGURED;
        }
        if (!properties.getPersistence().isDatabaseWriteEnabled()) {
            return CiqConfigurationStatus.RAW_READY_DATABASE_SCOPE_PENDING;
        }
        return CiqConfigurationStatus.READY;
    }

    private static boolean anyCollectorEnabled(CiqProperties.Collectors c) {
        return c.getTrafficSpeed().isEnabled()
                || c.getEstimatedTravelTimes().isEnabled()
                || c.getTrafficIncidents().isEnabled()
                || c.getVms().isEnabled()
                || c.getFaultyTrafficLights().isEnabled()
                || c.getRoadWorks().isEnabled()
                || c.getTrafficFlow().isEnabled()
                || c.getRoadOpenings().isEnabled();
    }

    private static boolean hasEnabledCollectorWithMissingConfiguration(CiqProperties.Collectors c) {
        return missing(c.getTrafficSpeed())
                || missing(c.getEstimatedTravelTimes())
                || missing(c.getTrafficIncidents())
                || missing(c.getVms())
                || missing(c.getFaultyTrafficLights())
                || missing(c.getRoadWorks())
                || missing(c.getTrafficFlow())
                || missing(c.getRoadOpenings());
    }

    private static boolean configured(CiqProperties.CollectorSettings s) {
        return s.isEnabled() && !blank(s.getEndpoint()) && !blank(s.getCron());
    }

    private static boolean missing(CiqProperties.CollectorSettings s) {
        return s.isEnabled() && (blank(s.getEndpoint()) || blank(s.getCron()));
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
