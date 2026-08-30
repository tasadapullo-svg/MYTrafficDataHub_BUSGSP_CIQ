package com.mytransitgps.modules.ciq.monitoring;

import com.mytransitgps.dashboard.provider.DashboardModuleProvider;
import com.mytransitgps.modules.ciq.config.CiqConfigurationStatus;
import com.mytransitgps.modules.ciq.config.CiqConfigurationValidator;
import com.mytransitgps.modules.ciq.config.CiqProperties;
import com.mytransitgps.platform.collection.ModuleCode;
import com.mytransitgps.platform.monitoring.ModuleMonitoringSnapshot;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/**
 * CIQ 模块只读监测 Provider。
 *
 * <p>当前不虚构请求量或入库量，只根据配置真实返回 DISABLED、NOT_CONFIGURED、COLLECTOR_DISABLED、
 * RAW_READY_DATABASE_SCOPE_PENDING 或 READY；后续数据库链完成后再接入真实采集统计。
 */
@Component
public class CiqDashboardProvider implements DashboardModuleProvider {
    private final CiqProperties properties;
    private final CiqConfigurationValidator validator;
    private final ObjectProvider<CiqMonitoringRepository> repositoryProvider;

    public CiqDashboardProvider(CiqProperties properties, CiqConfigurationValidator validator,
                                ObjectProvider<CiqMonitoringRepository> repositoryProvider) {
        this.properties = properties;
        this.validator = validator;
        this.repositoryProvider = repositoryProvider;
    }

    @Override
    public ModuleCode moduleCode() {
        return ModuleCode.CIQ;
    }

    @Override
    public ModuleMonitoringSnapshot getSnapshot() {
        CiqConfigurationStatus status = validator.status(properties);
        if (status != CiqConfigurationStatus.READY) {
            return ModuleMonitoringSnapshot.unavailable(ModuleCode.CIQ, "Singapore CIQ", status.name());
        }
        CiqMonitoringRepository repository = repositoryProvider.getIfAvailable();
        if (repository == null) {
            return ModuleMonitoringSnapshot.unavailable(ModuleCode.CIQ, "Singapore CIQ", "NOT_CONFIGURED");
        }
        try {
            Instant lastCollection = repository.lastCollectionTime();
            Instant latestData = repository.latestDataTime();
            long failures = repository.failureCount();
            String runtimeStatus = runtimeStatus(lastCollection, failures);
            return new ModuleMonitoringSnapshot(ModuleCode.CIQ, "Singapore CIQ", runtimeStatus,
                    lastCollection, repository.todayRecords(), repository.yesterdayRecords(),
                    repository.requestCount(), repository.successCount(), failures, latestData);
        } catch (DataAccessException ex) {
            return ModuleMonitoringSnapshot.unavailable(ModuleCode.CIQ, "Singapore CIQ", "ERROR");
        }
    }

    private static String runtimeStatus(Instant lastCollection, long failures) {
        if (lastCollection == null) return "READY";
        long ageMinutes = Duration.between(lastCollection, Instant.now()).toMinutes();
        if (failures > 0 && ageMinutes < 180) return "WARNING";
        return ageMinutes > 180 ? "WARNING" : "NORMAL";
    }
}
