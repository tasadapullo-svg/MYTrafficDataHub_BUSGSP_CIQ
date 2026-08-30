package com.mytransitgps.gtfs;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.google.transit.realtime.GtfsRealtime;
import com.mytransitgps.gtfs.client.GtfsHttpResult;
import com.mytransitgps.gtfs.config.GtfsFeedDefinition;
import com.mytransitgps.gtfs.service.GpsJumpQcService;
import com.mytransitgps.gtfs.model.StaticFeedData;
import com.mytransitgps.gtfs.service.JsonOutputService;
import com.mytransitgps.gtfs.service.RealtimeStaticEnrichmentService;
import com.mytransitgps.gtfs.util.HashUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SourceSpeedMappingTest {

    private final RealtimeStaticEnrichmentService service = new RealtimeStaticEnrichmentService(new JsonOutputService());
    private final GpsJumpQcService gpsJumpQcService = new GpsJumpQcService(100.0d, 130.0d, 600L);

    @Test
    void shouldKeepNullWhenSpeedAbsent() throws Exception {
        Map<String, Object> vehicle = firstVehicle(feedMessage(null));
        assertNull(vehicle.get("source_speed_raw"));
        assertEquals(false, vehicle.get("source_speed_present"));
    }

    @Test
    void shouldKeepZeroWithoutFiltering() throws Exception {
        Map<String, Object> vehicle = firstVehicle(feedMessage(0.0f));
        assertEquals(0.0f, ((Number) vehicle.get("source_speed_raw")).floatValue());
        assertEquals(true, vehicle.get("source_speed_present"));
    }

    @Test
    void shouldKeepSourceValueWithoutUnitConversion() throws Exception {
        Map<String, Object> vehicle = firstVehicle(feedMessage(15.0f));
        assertEquals(15.0f, ((Number) vehicle.get("source_speed_raw")).floatValue());
        assertEquals("GTFS_SPEC_MPS", vehicle.get("source_speed_unit_declared"));
        assertEquals("UNVERIFIED_PROVIDER_SEMANTICS", vehicle.get("source_speed_unit_interpretation"));
    }

    @Test
    void shouldKeepHighSourceSpeedWithoutFiltering() throws Exception {
        Map<String, Object> vehicle = firstVehicle(feedMessage(89.0f));
        assertEquals(89.0f, ((Number) vehicle.get("source_speed_raw")).floatValue());
    }

    @Test
    void shouldKeepDerivedSpeedIndependentFromSourceSpeed() throws Exception {
        Map<String, Object> vehicle = firstVehicle(feedMessage(89.0f));
        var jump = gpsJumpQcService.evaluate(1000L, 3.139d, 101.6869d, true, 1120L, 3.149d, 101.6969d, true);
        assertEquals(89.0f, ((Number) vehicle.get("source_speed_raw")).floatValue());
        assertEquals("NORMAL", jump.gpsJumpStatus());
        assertEquals(0, Double.compare(0.0d, vehicle.containsKey("derived_speed_kmh") && vehicle.get("derived_speed_kmh") instanceof Number n ? n.doubleValue() : 0.0d));
    }

    private Map<String, Object> firstVehicle(GtfsRealtime.FeedMessage message) throws Exception {
        var result = service.enrich("batch", feed(), message, new StaticFeedData(Map.of(), Map.of(), 0, 0, false, false, false, false, false, false, false, false), httpResult());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> vehicles = (List<Map<String, Object>>) result.root().get("vehicles");
        return vehicles.get(0);
    }

    private GtfsRealtime.FeedMessage feedMessage(Float speed) {
        GtfsRealtime.Position.Builder position = GtfsRealtime.Position.newBuilder()
                .setLatitude(3.139f)
                .setLongitude(101.6869f);
        if (speed != null) {
            position.setSpeed(speed);
        }
        return GtfsRealtime.FeedMessage.newBuilder()
                .setHeader(GtfsRealtime.FeedHeader.newBuilder().setGtfsRealtimeVersion("2.0"))
                .addEntity(GtfsRealtime.FeedEntity.newBuilder()
                        .setId("entity-1")
                        .setVehicle(GtfsRealtime.VehiclePosition.newBuilder()
                                .setVehicle(GtfsRealtime.VehicleDescriptor.newBuilder().setId("veh-1"))
                                .setPosition(position)
                                .setTimestamp(1787787267L)))
                .build();
    }

    private GtfsFeedDefinition feed() {
        return new GtfsFeedDefinition(
                "rapid-bus-kl", "kuala_lumpur", "Kuala_Lumpur", "Kuala Lumpur", "Prasarana", "rapid_bus",
                "https://example.invalid/realtime", "https://example.invalid/static", "kuala_lumpur_rapid_bus", true);
    }

    private GtfsHttpResult httpResult() {
        Instant now = Instant.parse("2026-08-27T00:00:00Z");
        byte[] body = new byte[]{1, 2, 3};
        return new GtfsHttpResult(
                "REALTIME",
                java.net.URI.create("https://example.invalid/realtime"),
                java.net.URI.create("https://example.invalid/realtime"),
                200,
                "application/octet-stream",
                1L,
                now,
                now,
                now,
                body,
                HashUtils.sha256Hex(body),
                0,
                Map.of(),
                null,
                null);
    }
}
