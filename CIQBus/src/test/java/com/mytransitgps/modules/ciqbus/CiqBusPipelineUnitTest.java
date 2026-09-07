package com.mytransitgps.modules.ciqbus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mytransitgps.modules.ciqbus.config.CiqBusProperties;
import com.mytransitgps.modules.ciqbus.domain.CiqBusActiveEvent;
import com.mytransitgps.modules.ciqbus.domain.CiqBusObservation;
import com.mytransitgps.modules.ciqbus.domain.CiqBusPassage;
import com.mytransitgps.modules.ciqbus.parser.CiqBusRouteStopResolver;
import com.mytransitgps.modules.ciqbus.parser.LtaBusArrivalParser;
import com.mytransitgps.modules.ciqbus.persistence.CiqBusRealtimeRepository;
import com.mytransitgps.modules.ciqbus.redis.CiqBusEventStateStore;
import com.mytransitgps.modules.ciqbus.service.CiqBusDailyExportService;
import com.mytransitgps.modules.ciqbus.service.CiqBusEventMatcher;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class CiqBusPipelineUnitTest {
    @Test
    void parserAcceptsOnlyTargetServicesAndAllBusSlots() {
        LtaBusArrivalParser parser = parser();
        List<CiqBusObservation> observations = parser.parse(json("""
                {"BusStopCode":"46101","Services":[
                  {"ServiceNo":"160","Operator":"SBST","NextBus":{"EstimatedArrival":"2026-09-07T09:10:00+08:00","Latitude":"1.4446","Longitude":"103.7677","VisitNumber":"1"},"NextBus2":{},"NextBus3":{}},
                  {"ServiceNo":"170","Operator":"SBST","NextBus":{"EstimatedArrival":"2026-09-07T09:11:00+08:00","Latitude":"1.4446","Longitude":"103.7677","VisitNumber":"1"},"NextBus2":{},"NextBus3":{}},
                  {"ServiceNo":"170X","Operator":"SBST","NextBus":{"EstimatedArrival":"2026-09-07T09:12:00+08:00","Latitude":"1.4446","Longitude":"103.7677","VisitNumber":"1"},"NextBus2":{},"NextBus3":{}},
                  {"ServiceNo":"950","Operator":"SMRT","NextBus":{"EstimatedArrival":"2026-09-07T09:13:00+08:00","Latitude":"1.4446","Longitude":"103.7677","VisitNumber":"1"},"NextBus2":{},"NextBus3":{}},
                  {"ServiceNo":"999","Operator":"TEST","NextBus":{"EstimatedArrival":"2026-09-07T09:14:00+08:00","Latitude":"1.4446","Longitude":"103.7677","VisitNumber":"1"},"NextBus2":{},"NextBus3":{}}
                ]}
                """), Instant.parse("2026-09-07T01:08:00Z"));
        assertEquals(List.of("160", "170", "170X", "950"), observations.stream().map(CiqBusObservation::serviceNo).toList());
        assertTrue(observations.stream().allMatch(o -> "SG_TO_JB".equals(o.directionCode())));
    }

    @Test
    void matcherReusesContinuousEventFiveTimesAndSetsTtl() {
        InMemoryStore store = new InMemoryStore();
        CiqBusEventMatcher matcher = new CiqBusEventMatcher(properties(), store);
        Instant t = Instant.parse("2026-09-07T01:00:00Z");
        String matched = null;
        for (int i = 0; i < 5; i++) {
            CiqBusObservation obs = observation("170", "SG_TO_JB", "46101", t.plusSeconds(i * 120), 1.4446 + i * 0.0001);
            CiqBusEventMatcher.MatchResult result = matcher.match(obs);
            if (matched == null) {
                matched = result.event().getMatchedEventId();
            }
            assertEquals(matched, result.event().getMatchedEventId());
        }
        assertEquals(Duration.ofMinutes(90).toSeconds(), store.lastActiveTtl.getSeconds());
    }

    @Test
    void matcherSeparatesSameRouteDifferentVehiclesAndGpsJump() {
        InMemoryStore store = new InMemoryStore();
        CiqBusEventMatcher matcher = new CiqBusEventMatcher(properties(), store);
        Instant t = Instant.parse("2026-09-07T01:00:00Z");
        String first = matcher.match(observation("170", "SG_TO_JB", "46101", t, 1.4446)).event().getMatchedEventId();
        String second = matcher.match(observation("170", "SG_TO_JB", "46101", t.plusSeconds(120), 1.50)).event().getMatchedEventId();
        assertNotEquals(first, second);
    }

    @Test
    void passageRedisGuardPreventsDuplicateStopButAllowsDifferentStops() {
        InMemoryStore store = new InMemoryStore();
        assertTrue(store.markPassageIfAbsent("e1", "46101", Duration.ofHours(4)));
        assertFalse(store.markPassageIfAbsent("e1", "46101", Duration.ofHours(4)));
        assertTrue(store.markPassageIfAbsent("e1", "46211", Duration.ofHours(4)));
        assertEquals(Duration.ofHours(4).toSeconds(), store.lastPassageTtl.getSeconds());
    }

    @Test
    void xlsxPathPreviousDayAndFourSheets() throws Exception {
        CiqBusProperties properties = properties();
        Path temp = Files.createTempDirectory("ciqbus-xlsx-test");
        properties.getStorage().setRoot(temp.toString());
        FakeRepository repository = new FakeRepository();
        CiqBusDailyExportService service = new CiqBusDailyExportService(repository, properties);
        Path output = service.exportPreviousDay(Instant.parse("2026-09-08T00:10:00+08:00"));
        assertEquals(temp.resolve("CIQBus_2026-09-07.xlsx"), output);
        try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(output))) {
            assertEquals("车辆站点明细", workbook.getSheetAt(0).getSheetName());
            assertEquals("完整过境事件", workbook.getSheetAt(1).getSheetName());
            assertEquals("小时统计", workbook.getSheetAt(2).getSheetName());
            assertEquals("每日汇总", workbook.getSheetAt(3).getSheetName());
        }
    }

    private LtaBusArrivalParser parser() {
        CiqBusProperties properties = properties();
        CiqBusRouteStopResolver resolver = new CiqBusRouteStopResolver(properties);
        return new LtaBusArrivalParser(new ObjectMapper(), properties, resolver);
    }

    private CiqBusProperties properties() {
        CiqBusProperties p = new CiqBusProperties();
        p.getLta().setTargetServices(List.of("160", "170", "170X", "950"));
        p.getLta().setStopCodes(List.of("46101", "46109", "46211", "46219"));
        List<CiqBusProperties.RouteStop> stops = new ArrayList<>();
        stops.add(stop("160", "1", "SG_TO_JB", "46101", 38, "ENTRY", 1.44460832436456, 103.76774882217022));
        stops.add(stop("160", "1", "SG_TO_JB", "46211", 39, "EXIT", 1.46491732433248, 103.76547712149443));
        stops.add(stop("170", "1", "SG_TO_JB", "46101", 64, "ENTRY", 1.44460832436456, 103.76774882217022));
        stops.add(stop("170", "1", "SG_TO_JB", "46211", 65, "EXIT", 1.46491732433248, 103.76547712149443));
        stops.add(stop("170", "2", "JB_TO_SG", "46219", 2, "ENTRY", 1.4654268105665, 103.76826657164715));
        stops.add(stop("170", "2", "JB_TO_SG", "46109", 3, "EXIT", 1.44694093553778, 103.76925342517902));
        stops.add(stop("170X", "1", "SG_TO_JB", "46101", 6, "ENTRY", 1.44460832436456, 103.76774882217022));
        stops.add(stop("950", "1", "SG_TO_JB", "46101", 7, "ENTRY", 1.44460832436456, 103.76774882217022));
        p.setRouteStops(stops);
        return p;
    }

    private CiqBusProperties.RouteStop stop(String route, String ltaDir, String direction, String code,
                                            int sequence, String type, double lat, double lon) {
        CiqBusProperties.RouteStop s = new CiqBusProperties.RouteStop();
        s.setRouteNo(route);
        s.setLtaDirection(ltaDir);
        s.setDirectionCode(direction);
        s.setStopCode(code);
        s.setStopName(code);
        s.setStopSequence(sequence);
        s.setPassageType(type);
        s.setLatitude(lat);
        s.setLongitude(lon);
        return s;
    }

    private CiqBusObservation observation(String route, String direction, String stop, Instant time, double lat) {
        return new CiqBusObservation(route, "SBST", direction, "1", stop, time.plusSeconds(60),
                lat, 103.7677, "SEA", "WAB", "SD", time, stop, "LTA_BUS_ARRIVAL");
    }

    private byte[] json(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    static class InMemoryStore implements CiqBusEventStateStore {
        final Map<String, CiqBusActiveEvent> active = new HashMap<>();
        final Map<String, Boolean> passage = new HashMap<>();
        Duration lastActiveTtl;
        Duration lastPassageTtl;

        public List<CiqBusActiveEvent> findActive(String routeNo, String directionCode) {
            return active.values().stream()
                    .filter(e -> routeNo.equals(e.getRouteNo()) && directionCode.equals(e.getDirectionCode()))
                    .toList();
        }

        public void saveActive(CiqBusActiveEvent event, Duration ttl) {
            active.put(event.getMatchedEventId(), event);
            lastActiveTtl = ttl;
        }

        public boolean markPassageIfAbsent(String matchedEventId, String stopCode, Duration ttl) {
            lastPassageTtl = ttl;
            return passage.putIfAbsent(matchedEventId + ":" + stopCode, Boolean.TRUE) == null;
        }

        public void markCompleted(String fingerprint, Duration ttl) {
        }

        public Long ttlSeconds(String key) {
            return lastActiveTtl == null ? -1 : lastActiveTtl.getSeconds();
        }
    }

    static class FakeRepository extends CiqBusRealtimeRepository {
        FakeRepository() {
            super(null);
        }

        public int upsertDailySummary(LocalDate date) {
            return 1;
        }

        public List<CiqBusPassage> passagesForDate(LocalDate date, ZoneId zoneId) {
            return List.of(new CiqBusPassage(UUID.randomUUID(), null, "m1", "170", "SBST",
                    "SG_TO_JB", "WOODLANDS", "46101", "W'lands Checkpt", 64, "ENTRY",
                    Instant.parse("2026-09-07T01:00:00Z"), Instant.parse("2026-09-07T01:01:00Z"),
                    Instant.parse("2026-09-07T00:59:00Z"), Instant.parse("2026-09-07T01:00:00Z"),
                    1.4446, 103.7677, "46101", 1, 0.9, "GPS_NEAR_STOP", "LTA_BUS_ARRIVAL"));
        }

        public List<CrossingRow> crossingsForDate(LocalDate date) {
            return List.of(new CrossingRow("m1", "170", "SG_TO_JB", "46101", "46211",
                    Instant.parse("2026-09-07T01:00:00Z"), Instant.parse("2026-09-07T01:10:00Z"),
                    600, 2, 0.9, "CONFIRMED"));
        }
    }
}
