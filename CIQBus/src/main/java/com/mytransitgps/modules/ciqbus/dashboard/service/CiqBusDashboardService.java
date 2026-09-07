package com.mytransitgps.modules.ciqbus.dashboard.service;

import com.mytransitgps.modules.ciqbus.config.CiqBusProperties;
import com.mytransitgps.modules.ciqbus.dashboard.dto.CiqBusDashboardDtos;
import com.mytransitgps.modules.ciqbus.dashboard.repository.CiqBusDashboardRepository;
import com.mytransitgps.modules.ciqbus.domain.CiqBusActiveEvent;
import com.mytransitgps.modules.ciqbus.redis.CiqBusEventStateStore;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CIQBus 大屏只读组合服务；Redis 和 PostgreSQL 查询均不修改业务数据。 */
@Service
@ConditionalOnProperty(prefix = "mytransitgps.database", name = "enabled", havingValue = "true")
@Transactional(readOnly = true, timeout = 30)
public class CiqBusDashboardService {
    private static final Logger log = LoggerFactory.getLogger(CiqBusDashboardService.class);
    private static final List<String> ROUTES = List.of("160", "170", "170X", "950");
    private static final List<String> DIRECTIONS = List.of("SG_TO_JB", "JB_TO_SG");

    private final CiqBusDashboardRepository repository;
    private final CiqBusEventStateStore stateStore;
    private final ZoneId zoneId;

    public CiqBusDashboardService(CiqBusDashboardRepository repository,
                                  ObjectProvider<CiqBusEventStateStore> stateStoreProvider,
                                  CiqBusProperties properties) {
        this.repository = repository;
        this.stateStore = stateStoreProvider.getIfAvailable();
        this.zoneId = ZoneId.of(properties.getTimezone());
    }

    public CiqBusDashboardDtos.RealtimeResponse realtime() {
        ActiveSnapshot active = activeSnapshot();
        return new CiqBusDashboardDtos.RealtimeResponse(active.available(), active.message(), Instant.now(),
                active.events().stream().map(this::toRealtimeEvent).toList());
    }

    public CiqBusDashboardDtos.Overview overview(LocalDate date) {
        LocalDate effective = date == null ? LocalDate.now(zoneId) : date;
        var row = repository.overview(effective);
        Map<String, Long> counts = new LinkedHashMap<>();
        ROUTES.forEach(route -> counts.put(route, 0L));
        repository.routeCounts(effective).forEach((route, count) -> {
            if (counts.containsKey(route)) counts.put(route, count);
        });
        ActiveSnapshot active = activeSnapshot();
        Integer activeVehicles = active.available() ? active.events().size() : null;
        Integer activeRoutes = active.available() ? (int) active.events().stream().map(CiqBusActiveEvent::getRouteNo).filter(ROUTES::contains).distinct().count() : null;
        return new CiqBusDashboardDtos.Overview(effective, row.sgToJb(), row.jbToSg(), row.total(), row.incompleteCount(),
                counts, activeVehicles, activeRoutes, row.avgSeconds(), row.medianSeconds(), row.p90Seconds(),
                row.p95Seconds(), Instant.now());
    }

    public List<CiqBusDashboardDtos.HourlyPoint> hourly(LocalDate date) {
        LocalDate effective = date == null ? LocalDate.now(zoneId) : date;
        Map<Integer, CiqBusDashboardRepository.HourlyRow> byHour = new LinkedHashMap<>();
        repository.hourly(effective, zoneId).forEach(row -> byHour.put(row.hour(), row));
        List<CiqBusDashboardDtos.HourlyPoint> result = new ArrayList<>(24);
        for (int hour = 0; hour < 24; hour++) {
            var row = byHour.get(hour);
            result.add(row == null
                    ? new CiqBusDashboardDtos.HourlyPoint(hour, String.format("%02d:00", hour), 0, 0, 0, null, null, null, null)
                    : new CiqBusDashboardDtos.HourlyPoint(hour, String.format("%02d:00", hour), row.sgToJb(), row.jbToSg(), row.total(),
                    row.avgSeconds(), row.medianSeconds(), row.p90Seconds(), row.p95Seconds()));
        }
        return result;
    }

    public CiqBusDashboardDtos.Page<CiqBusDashboardDtos.Passage> passages(LocalDate date, String route,
                                                                           String direction, int page, int size) {
        LocalDate effective = date == null ? LocalDate.now(zoneId) : date;
        String safeRoute = validateRoute(route);
        String safeDirection = validateDirection(direction);
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        long total = repository.countPassages(effective, zoneId, safeRoute, safeDirection);
        long offset = (long) safePage * safeSize;
        List<CiqBusDashboardDtos.Passage> items = offset >= total ? List.of() : repository.passages(effective, zoneId,
                safeRoute, safeDirection, safeSize, offset).stream().map(row -> new CiqBusDashboardDtos.Passage(
                row.matchedEventId(), row.routeNo(), row.operatorCode(), row.directionCode(), row.stopCode(),
                row.stopName(), row.passTime(), row.estimatedArrival(), row.latitude(), row.longitude(),
                row.observationCount(), row.confidence(), row.matchMethod(), row.createTime())).toList();
        return page(safePage, safeSize, total, items);
    }

    public CiqBusDashboardDtos.Page<CiqBusDashboardDtos.Crossing> crossings(LocalDate date, String route,
                                                                             String direction, int page, int size) {
        LocalDate effective = date == null ? LocalDate.now(zoneId) : date;
        String safeRoute = validateRoute(route);
        String safeDirection = validateDirection(direction);
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        long total = repository.countCrossings(effective, safeRoute, safeDirection);
        long offset = (long) safePage * safeSize;
        List<CiqBusDashboardDtos.Crossing> items = offset >= total ? List.of() : repository.crossings(effective,
                safeRoute, safeDirection, safeSize, offset).stream().map(row -> new CiqBusDashboardDtos.Crossing(
                row.matchedEventId(), row.routeNo(), row.directionCode(), row.entryStopCode(), row.entryTime(),
                row.exitStopCode(), row.exitTime(), row.crossingSeconds(), row.crossingSeconds() == null ? null : row.crossingSeconds() / 60.0,
                row.confidence(), row.eventStatus())).toList();
        return page(safePage, safeSize, total, items);
    }

    public CiqBusDashboardDtos.Status status() {
        ActiveSnapshot active = activeSnapshot();
        Instant latest = active.events().stream().map(CiqBusActiveEvent::getLastSeenTime).filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder()).orElse(null);
        if (latest == null) {
            try { latest = repository.latestActivity(); }
            catch (RuntimeException ex) { log.warn("[CIQBUS-DASHBOARD] 读取数据库最新活动时间失败", ex); }
        }
        Integer vehicleCount = active.available() ? active.events().size() : null;
        Integer routeCount = active.available() ? (int) active.events().stream().map(CiqBusActiveEvent::getRouteNo).distinct().count() : null;
        String state;
        String message = active.message();
        if (!active.available()) state = "REALTIME_UNAVAILABLE";
        else if (latest == null) state = "NO_DATA";
        else state = Duration.between(latest, Instant.now()).abs().toMinutes() <= 5 ? "NORMAL" : "STALE";
        return new CiqBusDashboardDtos.Status("LTA BusArrival", 120, ROUTES, latest, vehicleCount, routeCount,
                state, active.available(), message, Instant.now());
    }

    private ActiveSnapshot activeSnapshot() {
        if (stateStore == null) return new ActiveSnapshot(false, "Redis active event reader unavailable", List.of());
        try {
            Map<String, CiqBusActiveEvent> unique = new LinkedHashMap<>();
            for (String route : ROUTES) {
                for (String direction : DIRECTIONS) {
                    for (CiqBusActiveEvent event : stateStore.findActive(route, direction)) {
                        if (event == null || "COMPLETED".equalsIgnoreCase(event.getStatus())) continue;
                        if (!ROUTES.contains(event.getRouteNo()) || !DIRECTIONS.contains(event.getDirectionCode())) continue;
                        if (event.getMatchedEventId() != null) unique.put(event.getMatchedEventId(), event);
                    }
                }
            }
            List<CiqBusActiveEvent> events = unique.values().stream()
                    .sorted(Comparator.comparing(CiqBusActiveEvent::getLastSeenTime, Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
            return new ActiveSnapshot(true, null, events);
        } catch (RuntimeException ex) {
            log.warn("[CIQBUS-DASHBOARD] Redis active 只读查询失败", ex);
            return new ActiveSnapshot(false, "Redis active event data unavailable", List.of());
        }
    }

    private CiqBusDashboardDtos.RealtimeEvent toRealtimeEvent(CiqBusActiveEvent event) {
        return new CiqBusDashboardDtos.RealtimeEvent(event.getMatchedEventId(), event.getRouteNo(), event.getOperatorCode(),
                event.getDirectionCode(), event.getLastLatitude(), event.getLastLongitude(), event.getLastSeenTime(),
                event.getLastEstimatedArrival(), event.getLastSourceStopCode(), event.getEntryStopCode(), event.getExitStopCode(),
                event.getObservationCount(), event.getConfidence(), event.getStatus());
    }

    private String validateRoute(String route) {
        String value = route == null || route.isBlank() ? "ALL" : route.toUpperCase();
        if (!"ALL".equals(value) && !ROUTES.contains(value)) throw new IllegalArgumentException("Unsupported CIQBus route: " + route);
        return value;
    }

    private String validateDirection(String direction) {
        String value = direction == null || direction.isBlank() ? "ALL" : direction.toUpperCase();
        if (!"ALL".equals(value) && !DIRECTIONS.contains(value)) throw new IllegalArgumentException("Unsupported CIQBus direction: " + direction);
        return value;
    }

    private <T> CiqBusDashboardDtos.Page<T> page(int page, int size, long total, List<T> items) {
        int pages = total == 0 ? 0 : (int) Math.ceil(total / (double) size);
        return new CiqBusDashboardDtos.Page<>(page, size, total, pages, items);
    }

    private record ActiveSnapshot(boolean available, String message, List<CiqBusActiveEvent> events) { }
}
