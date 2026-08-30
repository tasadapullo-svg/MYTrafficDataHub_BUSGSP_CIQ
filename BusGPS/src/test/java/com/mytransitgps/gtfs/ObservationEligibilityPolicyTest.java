package com.mytransitgps.gtfs;

import java.util.LinkedHashSet;
import java.util.Set;

import com.mytransitgps.gtfs.service.ObservationEligibilityPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservationEligibilityPolicyTest {

    private final ObservationEligibilityPolicy policy = new ObservationEligibilityPolicy();

    @Test
    void shouldKeepNormalGpsEligible() {
        var result = policy.evaluate(Set.of(), "VALID_POSITION", "NORMAL");
        assertTrue(result.analysisEligible());
        assertTrue(result.spatialEligible());
    }

    @Test
    void shouldKeepSpeedZeroEligibleWhenPositionNormal() {
        var result = policy.evaluate(Set.of("STATIONARY"), "VALID_POSITION", "NORMAL");
        assertTrue(result.analysisEligible());
        assertTrue(result.spatialEligible());
    }

    @Test
    void shouldMarkGpsJumpAnalysisIneligibleButPreserveSpatialEligibility() {
        var flags = new LinkedHashSet<String>();
        flags.add("GPS_JUMP");
        var result = policy.evaluate(flags, "VALID_POSITION", "GPS_JUMP");
        assertFalse(result.analysisEligible());
        assertTrue(result.spatialEligible());
    }

    @Test
    void shouldMarkFutureTimestampAnalysisIneligible() {
        var result = policy.evaluate(Set.of("FUTURE_TIMESTAMP"), "VALID_POSITION", "NORMAL");
        assertFalse(result.analysisEligible());
        assertTrue(result.spatialEligible());
    }

    @Test
    void shouldMarkZeroZeroSpatialAndAnalysisIneligible() {
        var result = policy.evaluate(Set.of("ZERO_ZERO_POSITION"), "ZERO_ZERO_POSITION", "INVALID_POSITION");
        assertFalse(result.analysisEligible());
        assertFalse(result.spatialEligible());
    }

    @Test
    void shouldMarkInvalidWgs84SpatialAndAnalysisIneligible() {
        var result = policy.evaluate(Set.of("INVALID_WGS84"), "INVALID_WGS84", "INVALID_POSITION");
        assertFalse(result.analysisEligible());
        assertFalse(result.spatialEligible());
    }

    @Test
    void shouldKeepStationarySpeedZeroEligible() {
        var result = policy.evaluate(Set.of("STATIONARY"), "VALID_POSITION", "NORMAL");
        assertTrue(result.analysisEligible());
        assertTrue(result.spatialEligible());
    }

    @Test
    void shouldKeepRouteUnresolvedObservationPreservedForAnalysis() {
        var result = policy.evaluate(Set.of("ROUTE_UNRESOLVED"), "VALID_POSITION", "NORMAL");
        assertTrue(result.analysisEligible());
        assertTrue(result.spatialEligible());
    }
}
