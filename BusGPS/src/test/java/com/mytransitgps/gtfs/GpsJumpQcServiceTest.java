package com.mytransitgps.gtfs;

import com.mytransitgps.gtfs.model.GpsJumpQcResult;
import com.mytransitgps.gtfs.service.GpsJumpQcService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GpsJumpQcServiceTest {

    private final GpsJumpQcService service = new GpsJumpQcService(100.0d, 130.0d, 600L);

    @Test
    void shouldClassifyNormalSuspiciousJumpAndLongGap() {
        GpsJumpQcResult normal = service.evaluate(1000L, 1.0d, 103.0d, true, 1120L, 1.01d, 103.01d, true);
        assertEquals("NORMAL", normal.gpsJumpStatus());
        assertEquals("NORMAL_GAP", normal.gapStatus());

        GpsJumpQcResult suspicious = service.evaluate(1000L, 1.0d, 103.0d, true, 1120L, 1.035d, 103.0d, true);
        assertEquals("SUSPICIOUS_SPEED", suspicious.gpsJumpStatus());

        GpsJumpQcResult jump = service.evaluate(1000L, 1.0d, 103.0d, true, 1120L, 1.05d, 103.0d, true);
        assertEquals("GPS_JUMP", jump.gpsJumpStatus());

        GpsJumpQcResult longGap = service.evaluate(1000L, 1.0d, 103.0d, true, 1701L, 1.03d, 103.03d, true);
        assertEquals("LONG_GAP", longGap.gpsJumpStatus());
        assertNull(longGap.derivedSpeedKmh());
    }

    @Test
    void shouldSkipInvalidPositionAndNonPositiveDelta() {
        assertEquals("INVALID_POSITION", service.evaluate(1000L, 0.0d, 0.0d, false, 1100L, 1.0d, 103.0d, true).gpsJumpStatus());
        assertEquals("NOT_AVAILABLE", service.evaluate(1000L, 1.0d, 103.0d, true, 1000L, 1.0d, 103.0d, true).gpsJumpStatus());
    }
}
