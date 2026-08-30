package com.mytransitgps.modules.ciq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mytransitgps.modules.ciq.config.CiqConfigurationValidator;
import com.mytransitgps.modules.ciq.config.CiqProperties;
import com.mytransitgps.modules.ciq.monitoring.CiqDashboardProvider;
import com.mytransitgps.modules.ciq.monitoring.CiqMonitoringRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/** 验证 CIQ Dashboard Provider 使用独立 CIQ 监控仓库。 */
class CiqDashboardProviderTest {
    @Test
    void returnsDisabledWithoutDatabaseQueriesWhenCiqDisabled() {
        CiqMonitoringRepository repository = mock(CiqMonitoringRepository.class);
        CiqDashboardProvider provider = new CiqDashboardProvider(new CiqProperties(),
                new CiqConfigurationValidator(), providerOf(repository));

        var snapshot = provider.getSnapshot();

        assertEquals("DISABLED", snapshot.status());
        verifyNoInteractions(repository);
    }

    @Test
    void returnsNormalSnapshotFromRepositoryWhenReady() {
        CiqProperties properties = readyProperties();
        CiqMonitoringRepository repository = mock(CiqMonitoringRepository.class);
        when(repository.lastCollectionTime()).thenReturn(Instant.now());
        when(repository.latestDataTime()).thenReturn(Instant.now());
        when(repository.todayRecords()).thenReturn(10L);
        when(repository.yesterdayRecords()).thenReturn(8L);
        when(repository.requestCount()).thenReturn(2L);
        when(repository.successCount()).thenReturn(2L);
        when(repository.failureCount()).thenReturn(0L);
        CiqDashboardProvider provider = new CiqDashboardProvider(properties, new CiqConfigurationValidator(),
                providerOf(repository));

        var snapshot = provider.getSnapshot();

        assertEquals("NORMAL", snapshot.status());
        assertEquals(10L, snapshot.todayRecords());
        assertEquals(2L, snapshot.successCount());
    }

    private static CiqProperties readyProperties() {
        CiqProperties properties = new CiqProperties();
        properties.setEnabled(true);
        properties.getLta().setBaseUrl("https://datamall2.mytransport.sg/ltaodataservice");
        properties.getLta().setAccountKey("configured");
        properties.getStorage().setRootDirectory("CIQ");
        properties.getCollectors().getTrafficSpeed().setEnabled(true);
        properties.getCollectors().getTrafficSpeed().setEndpoint("/v4/TrafficSpeedBands");
        properties.getPersistence().setDatabaseWriteEnabled(true);
        return properties;
    }

    private static <T> ObjectProvider<T> providerOf(T value) {
        return new ObjectProvider<>() {
            @Override
            public T getObject(Object... args) { return value; }
            @Override
            public T getIfAvailable() { return value; }
            @Override
            public T getIfUnique() { return value; }
            @Override
            public T getObject() { return value; }
        };
    }
}
