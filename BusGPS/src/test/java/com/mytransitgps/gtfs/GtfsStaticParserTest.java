package com.mytransitgps.gtfs;

import java.nio.file.Files;

import com.mytransitgps.gtfs.model.StaticFeedData;
import com.mytransitgps.gtfs.parser.GtfsStaticParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GtfsStaticParserTest {

    @Test
    void shouldParseLocalJohorStaticZipFixture() throws Exception {
        GtfsStaticParser parser = new GtfsStaticParser();
        byte[] zipBytes = Files.readAllBytes(TestArtifactSupport.latestLegacyStaticZip());

        StaticFeedData staticFeedData = parser.parse(zipBytes);

        assertTrue(staticFeedData.routesPresent());
        assertTrue(staticFeedData.tripsPresent());
        assertTrue(staticFeedData.stopsPresent());
        assertTrue(staticFeedData.stopTimesPresent());
        assertTrue(staticFeedData.routes().size() > 0);
        assertTrue(staticFeedData.trips().size() > 0);
        assertTrue(staticFeedData.stopsCount() > 0);
        assertTrue(staticFeedData.stopTimesCount() > 0);
    }
}
