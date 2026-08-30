package com.mytransitgps.gtfs;

import com.mytransitgps.gtfs.model.FeedSpatialBounds;
import com.mytransitgps.gtfs.model.PositionQcResult;
import com.mytransitgps.gtfs.service.PositionQcService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PositionQcServiceTest {

    private final PositionQcService service = new PositionQcService();
    private final FeedSpatialBounds bounds = new FeedSpatialBounds("feed", 1, 1, 0, 2, 1, "REFERENCED_SHAPES_PLUS_STOPS", null, "sha", 1d, 2d, 100d, 101d, 10d, 0.9d, 2.1d, 99.9d, 101.1d);

    @Test
    void shouldClassifyMissingInvalidZeroZeroAndOutOfBounds() {
        PositionQcResult missing = service.evaluate(null, null, bounds);
        assertEquals("MISSING_POSITION", missing.positionQcStatus());

        PositionQcResult invalid = service.evaluate(95.0d, 100.0d, bounds);
        assertEquals("INVALID_WGS84", invalid.positionQcStatus());

        PositionQcResult zeroZero = service.evaluate(0.0d, 0.0d, bounds);
        assertEquals("ZERO_ZERO_POSITION", zeroZero.positionQcStatus());
        assertTrue(zeroZero.positionWgs84Valid());
        assertFalse(zeroZero.feedBoundsValid());

        PositionQcResult outside = service.evaluate(3.0d, 100.0d, bounds);
        assertEquals("OUT_OF_BOUNDS", outside.positionQcStatus());

        PositionQcResult valid = service.evaluate(1.5d, 100.5d, bounds);
        assertEquals("VALID_POSITION", valid.positionQcStatus());
        assertTrue(valid.feedBoundsValid());
    }

    @Test
    void shouldSkipOutOfBoundsWhenBoundsUnavailable() {
        FeedSpatialBounds unavailable = new FeedSpatialBounds("feed", 0, 0, 0, 0, 0, "BOUNDS_UNAVAILABLE", "NO_REFERENCED_SHAPE_POINTS", "sha", null, null, null, null, 10d, null, null, null, null);
        PositionQcResult result = service.evaluate(1.5d, 100.5d, unavailable);
        assertEquals("VALID_POSITION", result.positionQcStatus());
    }
}
