package com.mytransitgps.modules.ciqbus.service;

import com.mytransitgps.modules.ciqbus.config.CiqBusProperties;
import com.mytransitgps.modules.ciqbus.domain.CiqBusActiveEvent;
import com.mytransitgps.modules.ciqbus.domain.CiqBusObservation;
import com.mytransitgps.modules.ciqbus.redis.CiqBusEventStateStore;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CiqBusEventMatcher {
    private static final Logger log = LoggerFactory.getLogger(CiqBusEventMatcher.class);

    private final CiqBusProperties properties;
    private final CiqBusEventStateStore store;
    private final ZoneId zoneId;

    public CiqBusEventMatcher(CiqBusProperties properties, CiqBusEventStateStore store) {
        this.properties = properties;
        this.store = store;
        this.zoneId = ZoneId.of(properties.getTimezone());
    }

    public MatchResult match(CiqBusObservation observation) {
        log.info("[CIQBUS-MATCH] 项目进度：开始匹配车辆事件，routeNo={}，directionCode={}，sourceStopCode={}，estimatedArrival={}",
                observation.serviceNo(), observation.directionCode(), observation.sourceStopCode(), observation.estimatedArrival());
        List<CiqBusActiveEvent> candidates = store.findActive(observation.serviceNo(), observation.directionCode());
        Candidate best = candidates.stream()
                .filter(c -> !"COMPLETED".equals(c.getStatus()))
                .map(c -> new Candidate(c, score(c, observation)))
                .max(Comparator.comparingDouble(Candidate::score))
                .orElse(null);
        CiqBusActiveEvent event = best != null && best.score() >= properties.getMatching().getThreshold()
                ? best.event() : newEvent(observation);
        boolean created = best == null || best.score() < properties.getMatching().getThreshold();
        update(event, observation, created ? 0.60d : Math.min(0.98d, best.score() / 100.0d));
        store.saveActive(event, Duration.ofMinutes(properties.getRedis().getActiveTtlMinutes()));
        log.info("[CIQBUS-MATCH] 项目进度：车辆事件匹配完成，matchedEventId={}，是否新事件={}，候选数={}，匹配分数={}，观测次数={}",
                event.getMatchedEventId(), created, candidates.size(), created ? 0.0d : best.score(), event.getObservationCount());
        return new MatchResult(event, created, created ? 0.0d : best.score());
    }

    private double score(CiqBusActiveEvent event, CiqBusObservation observation) {
        if (!same(event.getRouteNo(), observation.serviceNo()) || !same(event.getDirectionCode(), observation.directionCode())) {
            return 0;
        }
        long minutes = Math.abs(Duration.between(event.getLastSeenTime(), observation.collectionTime()).toMinutes());
        if (minutes > properties.getMatching().getMaxCandidateAgeMinutes()) {
            return 0;
        }
        double score = 25;
        score += Math.max(0, 25 - minutes * 2);
        if (event.getLastEstimatedArrival() != null && observation.estimatedArrival() != null) {
            long etaDrift = Math.abs(Duration.between(event.getLastEstimatedArrival(), observation.estimatedArrival()).toMinutes());
            score += Math.max(0, 20 - etaDrift * 2);
        }
        if (event.getLastLatitude() != null && event.getLastLongitude() != null
                && observation.latitude() != null && observation.longitude() != null) {
            double km = haversineKm(event.getLastLatitude(), event.getLastLongitude(), observation.latitude(), observation.longitude());
            double hours = Math.max(1.0 / 60.0, Math.abs(Duration.between(event.getLastSeenTime(), observation.collectionTime()).toSeconds()) / 3600.0);
            double kmh = km / hours;
            if (kmh > properties.getMatching().getMaxGpsJumpKmh()) {
                return 0;
            }
            score += Math.max(0, 25 - km * 8);
        } else {
            score += 5;
        }
        if (same(event.getLastSourceStopCode(), observation.sourceStopCode())) {
            score += 10;
        }
        return Math.min(100, score);
    }

    private CiqBusActiveEvent newEvent(CiqBusObservation observation) {
        CiqBusActiveEvent event = new CiqBusActiveEvent();
        LocalDate date = LocalDate.ofInstant(observation.collectionTime(), zoneId);
        event.setMatchedEventId(observation.serviceNo() + "_" + observation.directionCode() + "_" + date + "_" + UUID.randomUUID().toString().substring(0, 8));
        event.setRouteNo(observation.serviceNo());
        event.setDirectionCode(observation.directionCode());
        event.setFirstSeenTime(observation.collectionTime());
        return event;
    }

    private void update(CiqBusActiveEvent event, CiqBusObservation observation, double confidence) {
        event.setOperatorCode(observation.operatorCode());
        event.setLastSeenTime(observation.collectionTime());
        event.setLastEstimatedArrival(observation.estimatedArrival());
        event.setLastLatitude(observation.latitude());
        event.setLastLongitude(observation.longitude());
        event.setLastSourceStopCode(observation.sourceStopCode());
        event.setObservationCount(event.getObservationCount() + 1);
        event.setConfidence(confidence);
    }

    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double earthKm = 6371.0088d;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return earthKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private boolean same(String a, String b) {
        return a != null && b != null && a.equalsIgnoreCase(b);
    }

    public record MatchResult(CiqBusActiveEvent event, boolean newEvent, double matchScore) {
    }

    private record Candidate(CiqBusActiveEvent event, double score) {
    }
}
