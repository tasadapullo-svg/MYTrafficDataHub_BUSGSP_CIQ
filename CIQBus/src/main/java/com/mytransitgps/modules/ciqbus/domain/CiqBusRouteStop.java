package com.mytransitgps.modules.ciqbus.domain;

public record CiqBusRouteStop(
        String routeNo,
        String ltaDirection,
        String directionCode,
        String stopCode,
        String stopName,
        Integer stopSequence,
        String passageType,
        Double latitude,
        Double longitude
) {
}
