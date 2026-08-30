package com.mytransitgps.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.mytransitgps.gtfs.config.GtfsFeedRegistry;
import com.mytransitgps.modules.ciq.client.LtaTrafficSpeedClient;
import com.mytransitgps.modules.ciq.collector.CiqTrafficSpeedCollector;
import com.mytransitgps.modules.ciq.config.CiqProperties;
import com.mytransitgps.modules.ciq.monitoring.CiqDashboardProvider;
import com.mytransitgps.modules.ciq.scheduler.CiqCollectionScheduler;
import com.mytransitgps.platform.config.BusGpsProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PlatformConfigurationTest {
    @Autowired BusGpsProperties busGpsProperties;
    @Autowired CiqProperties ciqProperties;
    @Autowired GtfsFeedRegistry configuredRegistry;
    @Autowired CiqDashboardProvider ciqDashboardProvider;
    @Autowired ApplicationContext context;

    @Test
    void configuredBusFeedsMustExactlyMatchStableLegacyDefaults() {
        assertEquals(5, busGpsProperties.getFeeds().size());
        assertEquals(new GtfsFeedRegistry().getAllFeeds(), configuredRegistry.getAllFeeds());
    }

    @Test
    void ciqMustBeDisabledAndScheduleMustKeepRequiredSemantics() {
        assertFalse(ciqProperties.isEnabled());
        assertFalse(ciqProperties.getSchedule().isEnabled());
        assertFalse(ciqProperties.getCollectors().getTrafficSpeed().isEnabled());
        assertEquals("0 0,15,30,45 * * * *", ciqProperties.getSchedule().getCron());
        assertEquals("Asia/Kuala_Lumpur", ciqProperties.getTimezone());
        assertEquals("CIQ", ciqProperties.getStorage().getRootDirectory());
        org.junit.jupiter.api.Assertions.assertTrue(ciqProperties.getArchive().isEnabled());
        assertFalse(ciqProperties.getPersistence().isDatabaseWriteEnabled());
        assertEquals("DISABLED", ciqDashboardProvider.getSnapshot().status());

        assertNull(context.getBeanProvider(LtaTrafficSpeedClient.class).getIfAvailable());
        assertNull(context.getBeanProvider(CiqTrafficSpeedCollector.class).getIfAvailable());
        assertNull(context.getBeanProvider(CiqCollectionScheduler.class).getIfAvailable());
    }
}
