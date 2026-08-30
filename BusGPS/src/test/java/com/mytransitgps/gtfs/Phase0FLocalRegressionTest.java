package com.mytransitgps.gtfs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.transit.realtime.GtfsRealtime;
import com.mytransitgps.gtfs.client.GtfsHttpResult;
import com.mytransitgps.gtfs.config.GtfsFeedDefinition;
import com.mytransitgps.gtfs.config.GtfsFeedRegistry;
import com.mytransitgps.gtfs.model.FeedSpatialBounds;
import com.mytransitgps.gtfs.model.GpsJumpQcResult;
import com.mytransitgps.gtfs.model.PositionQcResult;
import com.mytransitgps.gtfs.model.StaticFeedData;
import com.mytransitgps.gtfs.model.StaticShapeQcResult;
import com.mytransitgps.gtfs.parser.GtfsRealtimeParser;
import com.mytransitgps.gtfs.parser.GtfsStaticParser;
import com.mytransitgps.gtfs.service.FeedBoundsService;
import com.mytransitgps.gtfs.service.GpsJumpQcService;
import com.mytransitgps.gtfs.service.JsonOutputService;
import com.mytransitgps.gtfs.service.ObservationEligibilityPolicy;
import com.mytransitgps.gtfs.service.PositionQcService;
import com.mytransitgps.gtfs.service.RealtimeStaticEnrichmentService;
import com.mytransitgps.gtfs.service.StaticShapeQcService;
import com.mytransitgps.gtfs.util.HashUtils;
import com.mytransitgps.gtfs.util.StatsUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase0FLocalRegressionTest {

    private static final String RUN_ID = "one_hour_run_20260827_142338_d4d7";

    private final ObjectMapper objectMapper = new JsonOutputService().objectMapper();
    private final GtfsFeedRegistry feedRegistry = new GtfsFeedRegistry();
    private final GtfsRealtimeParser realtimeParser = new GtfsRealtimeParser();
    private final GtfsStaticParser staticParser = new GtfsStaticParser();
    private final RealtimeStaticEnrichmentService enrichmentService = new RealtimeStaticEnrichmentService(new JsonOutputService());
    private final FeedBoundsService feedBoundsService = new FeedBoundsService(10.0d);
    private final PositionQcService positionQcService = new PositionQcService();
    private final GpsJumpQcService gpsJumpQcService = new GpsJumpQcService(100.0d, 130.0d, 600L);
    private final ObservationEligibilityPolicy observationEligibilityPolicy = new ObservationEligibilityPolicy();
    private final StaticShapeQcService staticShapeQcService = new StaticShapeQcService(10.0d);
    private final JsonOutputService jsonOutputService = new JsonOutputService();

    @Test
    void shouldReplayOneHourLocallyWithoutNetworkAndPreserveAllObservations() throws Exception {
        Path outputRoot = Path.of("target", "regression-phase0f");
        Files.createDirectories(outputRoot);

        Map<String, FeedSpatialBounds> oldBoundsByFeed = loadOldBounds();
        Map<String, JsonNode> staticMetadataByFeed = loadStaticMetadataByFeed();
        Map<String, List<JsonNode>> realtimeMetadataByFeed = loadRealtimeMetadataByFeed();

        int totalVehicleCount = 0;
        int regressionVehicleCount = 0;
        Set<String> totalEntityIds = new LinkedHashSet<>();
        Set<String> regressionEntityIds = new LinkedHashSet<>();
        Map<String, Object> summary = new LinkedHashMap<>();
        List<Map<String, Object>> feedSummaries = new ArrayList<>();

        for (GtfsFeedDefinition feed : feedRegistry.getEnabledFeeds()) {
            JsonNode staticMetadata = staticMetadataByFeed.get(feed.feedId());
            assertNotNull(staticMetadata, "Missing static metadata for " + feed.feedId());

            Path staticZipPath = TestArtifactSupport.workspaceRoot().resolve(staticMetadata.path("static_object_path").asText());
            StaticFeedData staticFeedData = staticParser.parse(Files.readAllBytes(staticZipPath));
            FeedSpatialBounds newBounds = feedBoundsService.compute(feed.feedId(), staticMetadata.path("zip_sha256").asText(null), staticFeedData);
            StaticShapeQcResult staticShapeQcResult = staticShapeQcService.analyze(feed.feedId(), staticFeedData);

            FeedReplayState replayState = new FeedReplayState();
            FeedMetrics metrics = new FeedMetrics();
            List<JsonNode> realtimeMetadataRows = realtimeMetadataByFeed.getOrDefault(feed.feedId(), List.of()).stream()
                    .sorted(Comparator.comparing(node -> node.path("raw_pb_path").asText()))
                    .toList();

            for (JsonNode metadata : realtimeMetadataRows) {
                Path pbPath = TestArtifactSupport.workspaceRoot().resolve(metadata.path("raw_pb_path").asText());
                byte[] protobufBytes = Files.readAllBytes(pbPath);
                GtfsRealtime.FeedMessage feedMessage = realtimeParser.parse(protobufBytes);
                var enrichment = enrichmentService.enrich(RUN_ID, feed, feedMessage, staticFeedData, httpResult(feed, metadata, protobufBytes));
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> vehicles = objectMapper.convertValue(enrichment.root().get("vehicles"), new TypeReference<List<Map<String, Object>>>() {});
                annotateVehicles(feed.feedId(), newBounds, vehicles, Instant.parse(metadata.path("ingest_timestamp_utc").asText()), replayState, metrics);

                totalVehicleCount += feedMessage.getEntityCount();
                regressionVehicleCount += vehicles.size();
                totalEntityIds.addAll(entityIds(feedMessage));
                regressionEntityIds.addAll(vehicleEntityIds(vehicles));

                Path cityOutput = outputRoot.resolve(feed.cityFolder());
                String targetFileName = pbPath.getFileName().toString().replace(".pb", "_enriched_phase0f.json");
                jsonOutputService.writeJsonObject(cityOutput.resolve(targetFileName), Map.of("snapshot", enrichment.root().get("snapshot"), "vehicles", vehicles));
            }

            Map<String, Object> feedSummary = new LinkedHashMap<>();
            feedSummary.put("feed_id", feed.feedId());
            feedSummary.put("city", feed.cityName());
            feedSummary.put("vehicle_count", metrics.vehicleCount);
            feedSummary.put("entity_id_count", metrics.entityIds.size());
            feedSummary.put("out_of_bounds_count", metrics.outOfBoundsCount);
            feedSummary.put("gps_jump_count", metrics.gpsJumpCount);
            feedSummary.put("future_timestamp_count", metrics.futureTimestampCount);
            feedSummary.put("zero_zero_count", metrics.zeroZeroCount);
            feedSummary.put("source_speed_count", metrics.sourceSpeeds.size());
            feedSummary.put("source_speed_zero_count", metrics.sourceSpeedZeroCount);
            feedSummary.put("source_speed_min", StatsUtils.minDouble(metrics.sourceSpeeds));
            feedSummary.put("source_speed_p50", StatsUtils.percentileDouble(metrics.sourceSpeeds, 50));
            feedSummary.put("source_speed_p95", StatsUtils.percentileDouble(metrics.sourceSpeeds, 95));
            feedSummary.put("source_speed_max", StatsUtils.maxDouble(metrics.sourceSpeeds));
            feedSummary.put("derived_speed_min", StatsUtils.minDouble(metrics.derivedSpeeds));
            feedSummary.put("derived_speed_p50", StatsUtils.percentileDouble(metrics.derivedSpeeds, 50));
            feedSummary.put("derived_speed_p95", StatsUtils.percentileDouble(metrics.derivedSpeeds, 95));
            feedSummary.put("derived_speed_max", StatsUtils.maxDouble(metrics.derivedSpeeds));
            feedSummary.put("old_bounds", boundsMap(oldBoundsByFeed.get(feed.feedId())));
            feedSummary.put("new_bounds", boundsMap(newBounds));
            if ("mybas-kuching".equals(feed.feedId())) {
                FeedSpatialBounds oldBounds = oldBoundsByFeed.get(feed.feedId());
                feedSummary.put("kuching_orphan_present", hasOrphanShape(staticShapeQcResult, "SHP_30398_0"));
                feedSummary.put("kuching_orphan_excluded_from_bounds", oldBounds != null
                        && oldBounds.rawMaxLat() != null
                        && oldBounds.rawMinLon() != null
                        && newBounds.rawMaxLat() != null
                        && newBounds.rawMinLon() != null
                        && oldBounds.rawMaxLat() > newBounds.rawMaxLat()
                        && oldBounds.rawMinLon() < newBounds.rawMinLon());
                assertTrue(hasOrphanShape(staticShapeQcResult, "SHP_30398_0"));
                assertTrue(oldBounds.rawMaxLat() > newBounds.rawMaxLat());
                assertTrue(oldBounds.rawMinLon() < newBounds.rawMinLon());
            }
            feedSummaries.add(feedSummary);
        }

        summary.put("run_id", RUN_ID);
        summary.put("network_requests", 0);
        summary.put("original_vehicle_count", totalVehicleCount);
        summary.put("regression_enriched_count", regressionVehicleCount);
        summary.put("entity_id_match", totalEntityIds.equals(regressionEntityIds));
        summary.put("feed_summaries", feedSummaries);
        jsonOutputService.writeJsonObject(outputRoot.resolve("phase0f_regression_summary.json"), summary);

        assertEquals(11448, totalVehicleCount);
        assertEquals(totalVehicleCount, regressionVehicleCount);
        assertEquals(totalEntityIds, regressionEntityIds);
        assertTrue(feedSummaries.stream().anyMatch(row -> ((Number) row.get("gps_jump_count")).intValue() > 0));
        assertTrue(feedSummaries.stream().anyMatch(row -> ((Number) row.get("future_timestamp_count")).intValue() > 0));
        assertTrue(feedSummaries.stream().anyMatch(row -> ((Number) row.get("zero_zero_count")).intValue() > 0));
    }

    private Map<String, FeedSpatialBounds> loadOldBounds() throws IOException {
        Map<String, FeedSpatialBounds> results = new LinkedHashMap<>();
        JsonNode root = objectMapper.readTree(TestArtifactSupport.oneHourRunReportRoot().resolve("feed_spatial_bounds.json").toFile());
        for (JsonNode node : root) {
            results.put(node.path("feed_id").asText(), new FeedSpatialBounds(
                    node.path("feed_id").asText(),
                    node.path("total_shape_count").asInt(),
                    node.path("referenced_shape_count").asInt(),
                    node.path("orphan_shape_count").asInt(),
                    node.path("referenced_shape_point_count").asInt(),
                    node.path("stop_count").asInt(),
                    node.path("bounds_source").asText(null),
                    node.path("bounds_warning").isNull() ? null : node.path("bounds_warning").asText(null),
                    node.path("static_sha256").asText(null),
                    nullableDouble(node, "raw_min_lat"),
                    nullableDouble(node, "raw_max_lat"),
                    nullableDouble(node, "raw_min_lon"),
                    nullableDouble(node, "raw_max_lon"),
                    node.path("buffer_km").asDouble(10.0d),
                    nullableDouble(node, "effective_min_lat"),
                    nullableDouble(node, "effective_max_lat"),
                    nullableDouble(node, "effective_min_lon"),
                    nullableDouble(node, "effective_max_lon")));
        }
        return results;
    }

    private Map<String, JsonNode> loadStaticMetadataByFeed() throws IOException {
        Map<String, JsonNode> results = new LinkedHashMap<>();
        try (var stream = Files.walk(TestArtifactSupport.oneHourJsonRoot())) {
            for (Path path : stream.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().contains("_gtfs_static_metadata_"))
                    .toList()) {
                JsonNode node = objectMapper.readTree(path.toFile());
                if (RUN_ID.equals(node.path("run_id").asText())) {
                    results.put(node.path("feed_id").asText(), node);
                }
            }
        }
        return results;
    }

    private Map<String, List<JsonNode>> loadRealtimeMetadataByFeed() throws IOException {
        Map<String, List<JsonNode>> results = new LinkedHashMap<>();
        try (var stream = Files.walk(TestArtifactSupport.oneHourJsonRoot())) {
            for (Path path : stream.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().contains("_gtfs_realtime_metadata_"))
                    .toList()) {
                JsonNode node = objectMapper.readTree(path.toFile());
                if (RUN_ID.equals(node.path("run_id").asText())) {
                    results.computeIfAbsent(node.path("feed_id").asText(), ignored -> new ArrayList<>()).add(node);
                }
            }
        }
        return results;
    }

    private void annotateVehicles(
            String feedId,
            FeedSpatialBounds bounds,
            List<Map<String, Object>> vehicles,
            Instant ingestTimestampUtc,
            FeedReplayState replayState,
            FeedMetrics metrics) {
        for (Map<String, Object> vehicle : vehicles) {
            metrics.vehicleCount++;
            String observationKey = HashUtils.sha256Hex((
                    feedId + "|" +
                            string(vehicle.get("entity_id")) + "|" +
                            string(vehicle.get("vehicle_id")) + "|" +
                            string(vehicle.get("trip_id")) + "|" +
                            string(vehicle.get("realtime_route_id")) + "|" +
                            string(vehicle.get("latitude")) + "|" +
                            string(vehicle.get("longitude")) + "|" +
                            string(vehicle.get("vehicle_timestamp"))).getBytes());
            vehicle.put("observation_key", observationKey);

            String vehicleId = stringOrNull(vehicle.get("vehicle_id"));
            Long vehicleTimestamp = toLong(vehicle.get("vehicle_timestamp"));
            Double latitude = toDouble(vehicle.get("latitude"));
            Double longitude = toDouble(vehicle.get("longitude"));
            String observationIdentity = vehicleId != null && vehicleTimestamp != null
                    ? feedId + "|" + vehicleId + "|" + vehicleTimestamp
                    : observationKey;
            boolean duplicateObservation = !replayState.seenObservationKeys.add(observationIdentity);
            vehicle.put("duplicate_observation", duplicateObservation);

            LinkedHashSet<String> qcFlags = new LinkedHashSet<>();
            if (duplicateObservation) {
                qcFlags.add("DUPLICATE_OBSERVATION");
            }
            if (vehicleId == null) {
                qcFlags.add("MISSING_VEHICLE_ID");
            }
            if (vehicle.get("trip_id") == null) {
                qcFlags.add("MISSING_TRIP_ID");
            }
            if (vehicle.get("realtime_route_id") == null) {
                qcFlags.add("MISSING_ROUTE_ID");
            }
            if (!Boolean.TRUE.equals(vehicle.get("route_resolved"))) {
                qcFlags.add("ROUTE_UNRESOLVED");
            }
            if (!Boolean.TRUE.equals(vehicle.get("trip_static_matched"))) {
                qcFlags.add("TRIP_UNRESOLVED");
            }

            PositionQcResult positionQc = positionQcService.evaluate(latitude, longitude, bounds);
            vehicle.put("position_present", positionQc.positionPresent());
            vehicle.put("position_wgs84_valid", positionQc.positionWgs84Valid());
            vehicle.put("zero_zero_position", positionQc.zeroZeroPosition());
            vehicle.put("feed_bounds_valid", positionQc.feedBoundsValid());
            vehicle.put("position_qc_status", positionQc.positionQcStatus());
            if (!"VALID_POSITION".equals(positionQc.positionQcStatus())) {
                qcFlags.add(positionQc.positionQcStatus());
            }

            Long freshnessSeconds = vehicleTimestamp == null ? null : java.time.Duration.between(Instant.ofEpochSecond(vehicleTimestamp), ingestTimestampUtc).getSeconds();
            vehicle.put("freshness_seconds", freshnessSeconds);
            if (freshnessSeconds != null && freshnessSeconds < 0) {
                qcFlags.add("FUTURE_TIMESTAMP");
                metrics.futureTimestampCount++;
            }

            VehicleReplayState previous = vehicleId == null ? null : replayState.lastObservationByVehicleId.get(vehicleId);
            if (previous != null && previous.vehicleTimestamp != null && vehicleTimestamp != null) {
                if (vehicleTimestamp > previous.vehicleTimestamp && samePosition(previous, latitude, longitude)) {
                    qcFlags.add("STATIONARY");
                } else if (vehicleTimestamp.equals(previous.vehicleTimestamp)) {
                    qcFlags.add(samePosition(previous, latitude, longitude) ? "STALE_TIMESTAMP" : "INCONSISTENT_TIMESTAMP_POSITION");
                } else if (vehicleTimestamp < previous.vehicleTimestamp) {
                    qcFlags.add("TIMESTAMP_REGRESSION");
                }
            }

            GpsJumpQcResult jumpQc = gpsJumpQcService.evaluate(
                    previous == null ? null : previous.vehicleTimestamp,
                    previous == null ? null : previous.latitude,
                    previous == null ? null : previous.longitude,
                    previous != null && previous.positionValid,
                    vehicleTimestamp,
                    latitude,
                    longitude,
                    positionQc.transitPositionValid());
            vehicle.put("previous_vehicle_timestamp", jumpQc.previousVehicleTimestamp());
            vehicle.put("vehicle_gap_seconds", jumpQc.vehicleGapSeconds());
            vehicle.put("gap_status", jumpQc.gapStatus());
            vehicle.put("time_delta_seconds", jumpQc.timeDeltaSeconds());
            vehicle.put("distance_from_previous_m", jumpQc.distanceFromPreviousM());
            vehicle.put("derived_speed_kmh", jumpQc.derivedSpeedKmh());
            vehicle.put("gps_jump_status", jumpQc.gpsJumpStatus());
            vehicle.put("jump_calculation_skipped_reason", jumpQc.jumpCalculationSkippedReason());
            if ("LONG_GAP".equals(jumpQc.gapStatus())) {
                qcFlags.add("LONG_GAP");
            }
            if ("SUSPICIOUS_SPEED".equals(jumpQc.gpsJumpStatus())) {
                qcFlags.add("SUSPICIOUS_SPEED");
            } else if ("GPS_JUMP".equals(jumpQc.gpsJumpStatus())) {
                qcFlags.add("GPS_JUMP");
                metrics.gpsJumpCount++;
            }

            var eligibility = observationEligibilityPolicy.evaluate(qcFlags, positionQc.positionQcStatus(), jumpQc.gpsJumpStatus());
            vehicle.put("analysis_eligible", eligibility.analysisEligible());
            vehicle.put("spatial_eligible", eligibility.spatialEligible());
            vehicle.put("qc_flags", qcFlags.isEmpty() ? List.of("VALID") : List.copyOf(qcFlags));

            if ("OUT_OF_BOUNDS".equals(positionQc.positionQcStatus())) {
                metrics.outOfBoundsCount++;
            }
            if ("ZERO_ZERO_POSITION".equals(positionQc.positionQcStatus())) {
                metrics.zeroZeroCount++;
            }
            if (vehicle.get("entity_id") != null) {
                metrics.entityIds.add(vehicle.get("entity_id").toString());
            }
            if (vehicle.get("source_speed_raw") instanceof Number sourceSpeed) {
                metrics.sourceSpeeds.add(sourceSpeed.doubleValue());
                if (Double.compare(sourceSpeed.doubleValue(), 0.0d) == 0) {
                    metrics.sourceSpeedZeroCount++;
                }
            }
            if (vehicle.get("derived_speed_kmh") instanceof Number derivedSpeed) {
                metrics.derivedSpeeds.add(derivedSpeed.doubleValue());
            }

            if (vehicleId != null) {
                replayState.lastObservationByVehicleId.put(vehicleId, new VehicleReplayState(vehicleTimestamp, latitude, longitude, positionQc.transitPositionValid()));
            }
        }
    }

    private GtfsHttpResult httpResult(GtfsFeedDefinition feed, JsonNode metadata, byte[] protobufBytes) {
        return new GtfsHttpResult(
                "REALTIME",
                java.net.URI.create(metadata.path("requested_url").asText(feed.realtimeUrl())),
                java.net.URI.create(metadata.path("final_url").asText(feed.realtimeUrl())),
                metadata.path("http_status").asInt(200),
                metadata.path("content_type").asText("application/octet-stream"),
                metadata.path("latency_ms").asLong(0L),
                Instant.parse(metadata.path("request_started_at").asText()),
                Instant.parse(metadata.path("response_received_at").asText()),
                Instant.parse(metadata.path("ingest_timestamp_utc").asText()),
                protobufBytes,
                metadata.path("response_sha256").asText(HashUtils.sha256Hex(protobufBytes)),
                metadata.path("redirect_count").asInt(0),
                Map.of(),
                metadata.path("error_class").asText(null),
                metadata.path("error_message").asText(null));
    }

    private boolean hasOrphanShape(StaticShapeQcResult qcResult, String shapeId) {
        return qcResult.shapeDetails().stream().anyMatch(row ->
                shapeId.equals(row.get("shape_id")) && "ORPHAN_SHAPE".equals(row.get("referenced_status")));
    }

    private Set<String> entityIds(GtfsRealtime.FeedMessage feedMessage) {
        Set<String> ids = new LinkedHashSet<>();
        for (GtfsRealtime.FeedEntity entity : feedMessage.getEntityList()) {
            if (entity.hasVehicle() && entity.hasId()) {
                ids.add(entity.getId());
            }
        }
        return ids;
    }

    private Set<String> vehicleEntityIds(List<Map<String, Object>> vehicles) {
        Set<String> ids = new LinkedHashSet<>();
        for (Map<String, Object> vehicle : vehicles) {
            if (vehicle.get("entity_id") != null) {
                ids.add(vehicle.get("entity_id").toString());
            }
        }
        return ids;
    }

    private Map<String, Object> boundsMap(FeedSpatialBounds bounds) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (bounds == null) {
            return map;
        }
        map.put("bounds_source", bounds.boundsSource());
        map.put("raw_min_lat", bounds.rawMinLat());
        map.put("raw_max_lat", bounds.rawMaxLat());
        map.put("raw_min_lon", bounds.rawMinLon());
        map.put("raw_max_lon", bounds.rawMaxLon());
        map.put("effective_min_lat", bounds.effectiveMinLat());
        map.put("effective_max_lat", bounds.effectiveMaxLat());
        map.put("effective_min_lon", bounds.effectiveMinLon());
        map.put("effective_max_lon", bounds.effectiveMaxLon());
        map.put("referenced_shape_count", bounds.referencedShapeCount());
        map.put("orphan_shape_count", bounds.orphanShapeCount());
        return map;
    }

    private Double nullableDouble(JsonNode node, String fieldName) {
        return node.hasNonNull(fieldName) ? node.get(fieldName).asDouble() : null;
    }

    private boolean samePosition(VehicleReplayState previous, Double latitude, Double longitude) {
        return java.util.Objects.equals(previous.latitude, latitude)
                && java.util.Objects.equals(previous.longitude, longitude);
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return value == null ? null : Long.parseLong(value.toString());
    }

    private Double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return value == null ? null : Double.parseDouble(value.toString());
    }

    private String stringOrNull(Object value) {
        return value == null || value.toString().isBlank() ? null : value.toString();
    }

    private String string(Object value) {
        return value == null ? "" : value.toString();
    }

    private static class FeedReplayState {
        private final Set<String> seenObservationKeys = new LinkedHashSet<>();
        private final Map<String, VehicleReplayState> lastObservationByVehicleId = new LinkedHashMap<>();
    }

    private static class FeedMetrics {
        private int vehicleCount;
        private int outOfBoundsCount;
        private int gpsJumpCount;
        private int futureTimestampCount;
        private int zeroZeroCount;
        private int sourceSpeedZeroCount;
        private final Set<String> entityIds = new LinkedHashSet<>();
        private final List<Double> sourceSpeeds = new ArrayList<>();
        private final List<Double> derivedSpeeds = new ArrayList<>();
    }

    private static class VehicleReplayState {
        private final Long vehicleTimestamp;
        private final Double latitude;
        private final Double longitude;
        private final boolean positionValid;

        private VehicleReplayState(Long vehicleTimestamp, Double latitude, Double longitude, boolean positionValid) {
            this.vehicleTimestamp = vehicleTimestamp;
            this.latitude = latitude;
            this.longitude = longitude;
            this.positionValid = positionValid;
        }
    }
}
