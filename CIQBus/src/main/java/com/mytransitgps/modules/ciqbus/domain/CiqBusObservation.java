package com.mytransitgps.modules.ciqbus.domain;

import java.time.Instant;

public record CiqBusObservation(
        String serviceNo,
        String operatorCode,
        String directionCode,
        String ltaDirection,
        String sourceStopCode,
        Instant estimatedArrival,
        Double latitude,
        Double longitude,
        String load,
        String feature,
        String busType,
        Instant collectionTime,
        String targetStopCode,
        String sourceName
) {
}
