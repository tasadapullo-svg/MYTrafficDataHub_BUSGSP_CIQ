package com.mytransitgps.gtfs;

import java.util.List;
import java.util.Map;

import com.mytransitgps.gtfs.model.FeedSpatialBounds;
import com.mytransitgps.gtfs.model.ShapePoint;
import com.mytransitgps.gtfs.model.StaticFeedData;
import com.mytransitgps.gtfs.model.StopInfo;
import com.mytransitgps.gtfs.model.TripInfo;
import com.mytransitgps.gtfs.service.FeedBoundsService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeedBoundsServiceTest {

    @Test
    void shouldUseReferencedShapesAndStopsAndExcludeOrphans() {
        StaticFeedData data = new StaticFeedData(
                Map.of(),
                Map.of("trip-1", new TripInfo("route-1", "svc", "trip-1", null, null, 0, "shape-ref")),
                Map.of("A", new StopInfo("A", "A", 1.1d, 100.1d)),
                Map.of(),
                Map.of(
                        "shape-ref", List.of(
                                new ShapePoint("shape-ref", 1, 1.0d, 100.0d),
                                new ShapePoint("shape-ref", 2, 1.2d, 100.2d)),
                        "shape-orphan", List.of(
                                new ShapePoint("shape-orphan", 1, 8.0d, 120.0d),
                                new ShapePoint("shape-orphan", 2, 8.2d, 120.2d))),
                1,
                0,
                false,
                true,
                true,
                false,
                true,
                false,
                false,
                false);

        FeedSpatialBounds bounds = new FeedBoundsService(10.0d).compute("feed", "sha", data);

        assertEquals("REFERENCED_SHAPES_PLUS_STOPS", bounds.boundsSource());
        assertEquals(2, bounds.totalShapeCount());
        assertEquals(1, bounds.referencedShapeCount());
        assertEquals(1, bounds.orphanShapeCount());
        assertEquals(2, bounds.referencedShapePointCount());
        assertEquals(1, bounds.stopCount());
        assertTrue(bounds.contains(1.15d, 100.15d));
        assertFalse(bounds.contains(0.0d, 0.0d));
        assertFalse(bounds.contains(8.1d, 120.1d));
        assertTrue(bounds.effectiveMinLat() < 1.0d);
        assertTrue(bounds.effectiveMaxLon() > 100.2d);
    }

    @Test
    void shouldFallbackToStopsOnlyWhenReferencedShapesMissing() {
        StaticFeedData data = new StaticFeedData(
                Map.of(),
                Map.of("trip-1", new TripInfo("route-1", "svc", "trip-1", null, null, 0, "missing-shape")),
                Map.of("A", new StopInfo("A", "A", 2.0d, 101.0d)),
                Map.of(),
                Map.of(),
                1,
                0,
                false,
                true,
                true,
                false,
                false,
                false,
                false,
                false);

        FeedSpatialBounds bounds = new FeedBoundsService(5.0d).compute("feed", "sha", data);

        assertEquals("STOPS_ONLY", bounds.boundsSource());
        assertEquals("NO_REFERENCED_SHAPE_POINTS", bounds.boundsWarning());
        assertTrue(bounds.boundsAvailable());
        assertTrue(bounds.contains(2.0d, 101.0d));
    }

    @Test
    void shouldReturnUnavailableWhenNoReferencedShapesAndNoStops() {
        StaticFeedData data = new StaticFeedData(
                Map.of(),
                Map.of("trip-1", new TripInfo("route-1", "svc", "trip-1", null, null, 0, "missing-shape")),
                Map.of(),
                Map.of(),
                Map.of(),
                0,
                0,
                false,
                true,
                false,
                false,
                false,
                false,
                false,
                false);

        FeedSpatialBounds bounds = new FeedBoundsService(5.0d).compute("feed", "sha", data);

        assertEquals("BOUNDS_UNAVAILABLE", bounds.boundsSource());
        assertEquals("NO_REFERENCED_SHAPE_POINTS", bounds.boundsWarning());
        assertFalse(bounds.boundsAvailable());
        assertNull(bounds.rawMinLat());
        assertFalse(bounds.contains(1.0d, 101.0d));
    }
}
