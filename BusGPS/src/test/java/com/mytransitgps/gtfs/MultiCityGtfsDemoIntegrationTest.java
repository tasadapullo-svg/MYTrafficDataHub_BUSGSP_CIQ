package com.mytransitgps.gtfs;

import java.nio.file.Files;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.transit.realtime.GtfsRealtime;
import com.mytransitgps.gtfs.model.MultiCityDemoResult;
import com.mytransitgps.gtfs.model.MultiCityFeedRunResult;
import com.mytransitgps.gtfs.parser.GtfsRealtimeParser;
import com.mytransitgps.gtfs.service.JsonOutputService;
import com.mytransitgps.gtfs.service.MultiCityGtfsDemoRunner;
import com.mytransitgps.gtfs.util.GtfsTime;
import com.mytransitgps.gtfs.util.HashUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = "runIntegrationTests", matches = "true")
class MultiCityGtfsDemoIntegrationTest {

    private final GtfsRealtimeParser realtimeParser = new GtfsRealtimeParser();
    private final JsonOutputService jsonOutputService = new JsonOutputService();

    @Test
    void shouldRunSequentialMultiCityDemoAgainstRealApis() throws Exception {
        MultiCityGtfsDemoRunner runner = new MultiCityGtfsDemoRunner(TestArtifactSupport.workspaceRoot());

        MultiCityDemoResult result = runner.runDemo();

        assertEquals(5, result.feedResults().size());
        String batchDate = GtfsTime.formatBatchDate(result.batchDate());
        assertTrue(Files.exists(TestArtifactSupport.workspaceRoot().resolve("BusGPS").resolve(batchDate)));
        assertTrue(Files.exists(TestArtifactSupport.workspaceRoot().resolve("raw_data").resolve(batchDate)));
        assertTrue(Files.exists(result.fieldPresenceMatrixJsonPath()));
        assertTrue(Files.exists(result.fieldPresenceMatrixCsvPath()));

        for (MultiCityFeedRunResult feedResult : result.feedResults()) {
            assertEquals(200, feedResult.realtimeHttpResult().statusCode());
            assertEquals(200, feedResult.staticHttpResult().statusCode());
            assertNotNull(feedResult.rawPbPath());
            assertNotNull(feedResult.parsedJsonPath());
            assertNotNull(feedResult.enrichedJsonPath());
            assertNotNull(feedResult.realtimeMetadataPath());
            assertNotNull(feedResult.staticMetadataPath());
            assertTrue(Files.exists(feedResult.rawPbPath()));
            assertTrue(Files.exists(feedResult.parsedJsonPath()));
            assertTrue(Files.exists(feedResult.enrichedJsonPath()));
            assertTrue(Files.exists(feedResult.realtimeMetadataPath()));
            assertTrue(Files.exists(feedResult.staticMetadataPath()));

            JsonNode realtimeMetadata = jsonOutputService.objectMapper().readTree(feedResult.realtimeMetadataPath().toFile());
            assertEquals(Files.size(feedResult.rawPbPath()), realtimeMetadata.path("response_bytes").asLong());
            assertEquals(HashUtils.sha256Hex(Files.readAllBytes(feedResult.rawPbPath())), realtimeMetadata.path("response_sha256").asText());

            GtfsRealtime.FeedMessage parsedFromRaw = realtimeParser.parse(Files.readAllBytes(feedResult.rawPbPath()));
            long vehicleEntities = parsedFromRaw.getEntityList().stream().filter(GtfsRealtime.FeedEntity::hasVehicle).count();
            assertEquals(parsedFromRaw.getEntityCount(), realtimeMetadata.path("entity_count").asInt());
            assertEquals(vehicleEntities, realtimeMetadata.path("vehicle_count").asLong());

            JsonNode enriched = jsonOutputService.objectMapper().readTree(feedResult.enrichedJsonPath().toFile());
            assertEquals(vehicleEntities, enriched.path("vehicles").size());

            Set<String> parsedEntityIds = new HashSet<>();
            for (GtfsRealtime.FeedEntity entity : parsedFromRaw.getEntityList()) {
                if (entity.hasVehicle() && entity.hasId()) {
                    parsedEntityIds.add(entity.getId());
                }
            }
            for (JsonNode vehicle : enriched.path("vehicles")) {
                assertFalse(vehicle.path("entity_id").asText().isBlank());
                assertTrue(parsedEntityIds.contains(vehicle.path("entity_id").asText()));
                assertTrue(vehicle.has("realtime_entity"));
            }
        }
    }
}
