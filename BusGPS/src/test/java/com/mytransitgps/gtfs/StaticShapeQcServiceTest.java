package com.mytransitgps.gtfs;

import java.util.List;
import java.util.Map;

import com.mytransitgps.gtfs.model.RouteInfo;
import com.mytransitgps.gtfs.model.ShapePoint;
import com.mytransitgps.gtfs.model.StaticFeedData;
import com.mytransitgps.gtfs.model.StaticShapeQcResult;
import com.mytransitgps.gtfs.model.TripInfo;
import com.mytransitgps.gtfs.service.StaticShapeQcService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaticShapeQcServiceTest {

    @Test
    void shouldDetectReferencedOrphanInvalidAndSegmentJumpShapes() {
        StaticFeedData data = new StaticFeedData(
                Map.of("R1", new RouteInfo("R1", "R1", "Route 1", "3", null, null)),
                Map.of(
                        "T1", new TripInfo("R1", "WD", "T1", null, null, 0, "shape-ref"),
                        "T2", new TripInfo("R1", "WD", "T2", null, null, 1, "shape-missing")),
                Map.of(),
                Map.of(),
                Map.of(
                        "shape-ref", List.of(
                                new ShapePoint("shape-ref", 1, 1.0d, 103.0d),
                                new ShapePoint("shape-ref", 2, 1.0d, 103.0d),
                                new ShapePoint("shape-ref", 2, 1.5d, 110.0d)),
                        "shape-orphan", List.of(new ShapePoint("shape-orphan", 1, 0.0d, 0.0d))),
                0,
                0,
                true,
                true,
                false,
                false,
                true,
                false,
                false,
                false);

        StaticShapeQcResult result = new StaticShapeQcService(10.0d).analyze("feed", data);

        assertEquals(2, result.summary().get("shapes_total"));
        assertEquals(1, result.summary().get("orphan_shape_count"));
        assertEquals(1, result.summary().get("missing_referenced_shape_count"));
        assertEquals(2, result.summary().get("invalid_shape_count"));
        assertEquals(2, result.routeShapeLengths().size());
        assertTrue(result.shapeDetails().stream().anyMatch(row -> row.get("qc_flags").toString().contains("SHAPE_SEGMENT_JUMP")));
    }
}
