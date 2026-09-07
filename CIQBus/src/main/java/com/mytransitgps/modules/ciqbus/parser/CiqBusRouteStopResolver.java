package com.mytransitgps.modules.ciqbus.parser;

import com.mytransitgps.modules.ciqbus.config.CiqBusProperties;
import com.mytransitgps.modules.ciqbus.domain.CiqBusObservation;
import com.mytransitgps.modules.ciqbus.domain.CiqBusRouteStop;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class CiqBusRouteStopResolver {
    private final List<CiqBusRouteStop> routeStops;

    public CiqBusRouteStopResolver(CiqBusProperties properties) {
        this.routeStops = properties.getRouteStops().stream()
                .map(s -> new CiqBusRouteStop(s.getRouteNo(), s.getLtaDirection(), s.getDirectionCode(),
                        s.getStopCode(), s.getStopName(), s.getStopSequence(), s.getPassageType(),
                        s.getLatitude(), s.getLongitude()))
                .toList();
    }

    public Optional<CiqBusRouteStop> resolve(CiqBusObservation observation) {
        List<CiqBusRouteStop> exact = routeStops.stream()
                .filter(s -> same(s.routeNo(), observation.serviceNo()))
                .filter(s -> same(s.stopCode(), observation.sourceStopCode()))
                .filter(s -> same(s.ltaDirection(), observation.ltaDirection()))
                .toList();
        if (exact.size() == 1) {
            return Optional.of(exact.get(0));
        }
        return routeStops.stream()
                .filter(s -> same(s.routeNo(), observation.serviceNo()))
                .filter(s -> same(s.stopCode(), observation.sourceStopCode()))
                .min(Comparator.comparingInt(s -> s.stopSequence() == null ? Integer.MAX_VALUE : s.stopSequence()));
    }

    public Optional<CiqBusRouteStop> find(String routeNo, String directionCode, String passageType) {
        return routeStops.stream()
                .filter(s -> same(s.routeNo(), routeNo))
                .filter(s -> same(s.directionCode(), directionCode))
                .filter(s -> same(s.passageType(), passageType))
                .min(Comparator.comparingInt(s -> s.stopSequence() == null ? Integer.MAX_VALUE : s.stopSequence()));
    }

    public List<CiqBusRouteStop> routeStops() {
        return routeStops;
    }

    private boolean same(String a, String b) {
        return a != null && b != null && a.equalsIgnoreCase(b);
    }
}
