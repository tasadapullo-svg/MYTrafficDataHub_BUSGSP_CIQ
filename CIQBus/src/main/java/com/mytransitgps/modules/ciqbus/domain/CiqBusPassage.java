package com.mytransitgps.modules.ciqbus.domain;

import java.time.Instant;
import java.util.UUID;

public record CiqBusPassage(
        UUID uuid,
        UUID crossingEventUuid,
        String matchedEventId,
        String routeNo,
        String operatorCode,
        String directionCode,
        String ciqCode,
        String stopCode,
        String stopName,
        Integer stopSequence,
        String passageType,
        Instant passTime,
        Instant estimatedArrival,
        Instant firstSeenTime,
        Instant lastSeenTime,
        Double latitude,
        Double longitude,
        String sourceStopCode,
        int observationCount,
        double confidence,
        String matchMethod,
        String sourceName
) {
}
