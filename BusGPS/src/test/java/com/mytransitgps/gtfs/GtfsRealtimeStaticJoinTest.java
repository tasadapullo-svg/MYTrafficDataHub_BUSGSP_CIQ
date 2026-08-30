package com.mytransitgps.gtfs;

import java.nio.file.Files;
import java.time.Instant;
import java.util.Map;

import com.google.transit.realtime.GtfsRealtime;
import com.mytransitgps.gtfs.client.GtfsHttpResult;
import com.mytransitgps.gtfs.config.GtfsFeedDefinition;
import com.mytransitgps.gtfs.model.RouteInfo;
import com.mytransitgps.gtfs.model.StaticFeedData;
import com.mytransitgps.gtfs.model.TripInfo;
import com.mytransitgps.gtfs.parser.GtfsRealtimeParser;
import com.mytransitgps.gtfs.parser.GtfsStaticParser;
import com.mytransitgps.gtfs.service.FieldPresenceAuditService;
import com.mytransitgps.gtfs.service.JsonOutputService;
import com.mytransitgps.gtfs.service.RealtimeStaticEnrichmentService;
import com.mytransitgps.gtfs.util.HashUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GtfsRealtimeStaticJoinTest {

    private final RealtimeStaticEnrichmentService enrichmentService =
            new RealtimeStaticEnrichmentService(new JsonOutputService());
    private final FieldPresenceAuditService auditService = new FieldPresenceAuditService();

    @Test
    void shouldEnrichLocalJohorFixturesWithoutNetwork() throws Exception {
        GtfsRealtimeParser realtimeParser = new GtfsRealtimeParser();
        GtfsStaticParser staticParser = new GtfsStaticParser();

        byte[] protobufBytes = Files.readAllBytes(TestArtifactSupport.latestLegacyRealtimePb());
        byte[] staticZipBytes = Files.readAllBytes(TestArtifactSupport.latestLegacyStaticZip());
        GtfsRealtime.FeedMessage feedMessage = realtimeParser.parse(protobufBytes);
        StaticFeedData staticFeedData = staticParser.parse(staticZipBytes);

        var enrichment = enrichmentService.enrich("20260827_000000_test", johorFeed(), feedMessage, staticFeedData, httpResult(johorFeed(), protobufBytes));
        var audit = auditService.audit(johorFeed().feedId(), feedMessage);

        assertEquals(feedMessage.getEntityCount(), ((java.util.List<?>) enrichment.root().get("vehicles")).size());
        assertTrue(enrichment.vehicleCount() > 0);
        assertTrue(enrichment.routeResolvedCount() > 0);
        assertTrue(enrichment.tripMatchCount() > 0);
        assertEquals(enrichment.vehicleCount(), audit.vehicleCount());
    }

    @Test
    void shouldResolveKuchingRouteViaTripFallback() throws Exception {
        GtfsFeedDefinition feed = new GtfsFeedDefinition(
                "mybas-kuching", "kuching", "Kuching", "Kuching", "BAS.MY", "bus",
                "https://api.data.gov.my/gtfs-realtime/vehicle-position/mybas-kuching/",
                "https://api.data.gov.my/gtfs-static/mybas-kuching",
                "kuching", true);
        StaticFeedData staticFeedData = new StaticFeedData(
                Map.of("30399", new RouteInfo("30399", "Q10", "OPEN AIR MARKET - TERMINAL BAS SERIAN", "3", null, null)),
                Map.of("206_1_WD_4", new TripInfo("30399", "WD", "206_1_WD_4", null, null, 0, null)),
                0, 0, true, true, false, false, false, false, false, false);

        GtfsRealtime.FeedMessage feedMessage = GtfsRealtime.FeedMessage.newBuilder()
                .setHeader(GtfsRealtime.FeedHeader.newBuilder().setGtfsRealtimeVersion("2.0"))
                .addEntity(GtfsRealtime.FeedEntity.newBuilder()
                        .setId("MADANI1047")
                        .setVehicle(GtfsRealtime.VehiclePosition.newBuilder()
                                .setTrip(GtfsRealtime.TripDescriptor.newBuilder().setTripId("206_1_WD_4"))
                                .setVehicle(GtfsRealtime.VehicleDescriptor.newBuilder().setId("MADANI1047").setLabel("MADANI1047"))
                                .setPosition(GtfsRealtime.Position.newBuilder().setLatitude(1.1659667f).setLongitude(110.56663f))
                                .setTimestamp(1787787171L)))
                .build();

        var enrichment = enrichmentService.enrich("batch", feed, feedMessage, staticFeedData, httpResult(feed, new byte[]{1, 2, 3}));
        @SuppressWarnings("unchecked")
        Map<String, Object> vehicle = ((java.util.List<Map<String, Object>>) enrichment.root().get("vehicles")).get(0);

        assertNull(vehicle.get("realtime_route_id"));
        assertEquals("30399", vehicle.get("static_route_id"));
        assertEquals("30399", vehicle.get("resolved_route_id"));
        assertEquals("TRIP_TO_STATIC_ROUTE", vehicle.get("route_resolution_method"));
        assertEquals("Q10", vehicle.get("route_short_name"));
        assertTrue((Boolean) vehicle.get("route_resolved"));
        assertNull(vehicle.get("source_speed_raw"));
        assertEquals(false, vehicle.get("source_speed_present"));
        assertEquals("GTFS_SPEC_MPS", vehicle.get("source_speed_unit_declared"));
        assertEquals("UNVERIFIED_PROVIDER_SEMANTICS", vehicle.get("source_speed_unit_interpretation"));
        assertEquals(0, enrichment.routeDirectMatchCount());
        assertEquals(1, enrichment.routeTripFallbackMatchCount());
    }

    @Test
    void shouldResolveMrtRouteViaTripFallbackAndKeepDirectionNotComparable() throws Exception {
        GtfsFeedDefinition feed = new GtfsFeedDefinition(
                "rapid-bus-mrtfeeder", "kuala_lumpur", "Kuala_Lumpur", "Kuala Lumpur", "Prasarana", "mrt_feeder",
                "https://api.data.gov.my/gtfs-realtime/vehicle-position/prasarana?category=rapid-bus-mrtfeeder",
                "https://api.data.gov.my/gtfs-static/prasarana?category=rapid-bus-mrtfeeder",
                "kuala_lumpur_mrt_feeder", true);
        StaticFeedData staticFeedData = new StaticFeedData(
                Map.of("30000060", new RouteInfo("30000060", "T802", "T802", "3", null, null)),
                Map.of("260731010025S3", new TripInfo("30000060", "WD", "260731010025S3", null, null, 0, null)),
                0, 0, true, true, false, false, false, false, false, false);

        GtfsRealtime.FeedMessage feedMessage = GtfsRealtime.FeedMessage.newBuilder()
                .setHeader(GtfsRealtime.FeedHeader.newBuilder().setGtfsRealtimeVersion("2.0"))
                .addEntity(GtfsRealtime.FeedEntity.newBuilder()
                        .setId("152")
                        .setVehicle(GtfsRealtime.VehiclePosition.newBuilder()
                                .setTrip(GtfsRealtime.TripDescriptor.newBuilder().setTripId("260731010025S3").setRouteId("T802"))
                                .setVehicle(GtfsRealtime.VehicleDescriptor.newBuilder().setId("VAC5746").setLicensePlate("VAC5746"))
                                .setPosition(GtfsRealtime.Position.newBuilder().setLatitude(3.18419f).setLongitude(101.53583f))
                                .setTimestamp(1787787267L)))
                .build();

        var enrichment = enrichmentService.enrich("batch", feed, feedMessage, staticFeedData, httpResult(feed, new byte[]{4, 5, 6}));
        @SuppressWarnings("unchecked")
        Map<String, Object> vehicle = ((java.util.List<Map<String, Object>>) enrichment.root().get("vehicles")).get(0);

        assertEquals("T802", vehicle.get("realtime_route_id"));
        assertEquals("30000060", vehicle.get("static_route_id"));
        assertEquals("30000060", vehicle.get("resolved_route_id"));
        assertEquals("TRIP_TO_STATIC_ROUTE", vehicle.get("route_resolution_method"));
        assertEquals("NOT_COMPARABLE", vehicle.get("direction_comparison"));
        assertFalse((Boolean) vehicle.get("source_speed_present"));
        assertEquals(0, enrichment.directionMismatchCount());
        assertEquals(1, enrichment.directionNotComparableCount());
    }

    private GtfsFeedDefinition johorFeed() {
        return new GtfsFeedDefinition(
                "mybas-johor", "johor_bahru", "Johor_Bahru", "Johor Bahru", "BAS.MY", "bus",
                "https://api.data.gov.my/gtfs-realtime/vehicle-position/mybas-johor/",
                "https://api.data.gov.my/gtfs-static/mybas-johor", "johor_bahru", true);
    }

    private GtfsHttpResult httpResult(GtfsFeedDefinition feed, byte[] bytes) {
        Instant now = Instant.parse("2026-08-27T00:00:00Z");
        return new GtfsHttpResult(
                "REALTIME",
                java.net.URI.create(feed.realtimeUrl()),
                java.net.URI.create(feed.realtimeUrl()),
                200,
                "application/octet-stream",
                123L,
                now,
                now,
                now,
                bytes,
                HashUtils.sha256Hex(bytes),
                0,
                Map.of(),
                null,
                null);
    }
}
