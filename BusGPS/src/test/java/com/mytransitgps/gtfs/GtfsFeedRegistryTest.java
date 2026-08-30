package com.mytransitgps.gtfs;

import com.mytransitgps.gtfs.config.GtfsFeedRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GtfsFeedRegistryTest {

    @Test
    void shouldExposeFiveConfiguredFeeds() {
        GtfsFeedRegistry registry = new GtfsFeedRegistry();

        assertEquals(5, registry.getAllFeeds().size());
        assertEquals(5, registry.getEnabledFeeds().size());
        assertTrue(registry.findByFeedId("mybas-johor").isPresent());
        assertTrue(registry.findByFeedId("mybas-kuching").isPresent());
        assertTrue(registry.findByFeedId("rapid-bus-kl").isPresent());
        assertTrue(registry.findByFeedId("rapid-bus-mrtfeeder").isPresent());
        assertTrue(registry.findByFeedId("mybas-melaka").isPresent());
    }

    @Test
    void shouldKeepKualaLumpurFeedsInSameCityFolder() {
        GtfsFeedRegistry registry = new GtfsFeedRegistry();

        String rapidBusFolder = registry.findByFeedId("rapid-bus-kl").orElseThrow().cityFolder();
        String mrtFeederFolder = registry.findByFeedId("rapid-bus-mrtfeeder").orElseThrow().cityFolder();

        assertEquals("Kuala_Lumpur", rapidBusFolder);
        assertEquals(rapidBusFolder, mrtFeederFolder);
    }
}
