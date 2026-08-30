package com.mytransitgps.gtfs;

import java.util.List;
import java.util.Map;

import com.mytransitgps.gtfs.model.RouteInfo;
import com.mytransitgps.gtfs.model.StaticFeedData;
import com.mytransitgps.gtfs.model.StopTimeInfo;
import com.mytransitgps.gtfs.model.TripInfo;
import com.mytransitgps.gtfs.service.ScheduledTripTimeValidationService;
import com.mytransitgps.gtfs.util.GtfsTimeParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScheduledTripTimeValidationServiceTest {

    @Test
    void shouldClassifyTripTimeStatusesIncludingOver24hAndInvalidPatterns() {
        StaticFeedData data = new StaticFeedData(
                Map.of("R1", new RouteInfo("R1", "R1", "Route 1", "3", null, null)),
                Map.of(
                        "valid", new TripInfo("R1", "WD", "valid", null, null, 0, "shape1"),
                        "over24", new TripInfo("R1", "WD", "over24", null, null, 0, "shape1"),
                        "zero", new TripInfo("R1", "WD", "zero", null, null, 0, "shape1"),
                        "same", new TripInfo("R1", "WD", "same", null, null, 0, "shape1"),
                        "nonmono", new TripInfo("R1", "WD", "nonmono", null, null, 0, "shape1"),
                        "missing", new TripInfo("R1", "WD", "missing", null, null, 0, "shape1")),
                Map.of(),
                Map.of(
                        "valid", List.of(new StopTimeInfo("valid", 1, "A", "08:00:00", "08:00:30"), new StopTimeInfo("valid", 2, "B", "08:20:00", "08:20:30")),
                        "over24", List.of(new StopTimeInfo("over24", 1, "A", "24:10:00", "24:10:00"), new StopTimeInfo("over24", 2, "B", "25:10:00", "25:10:00")),
                        "zero", List.of(new StopTimeInfo("zero", 1, "A", "09:00:00", "09:00:00"), new StopTimeInfo("zero", 2, "B", "09:00:00", "09:00:00")),
                        "same", List.of(new StopTimeInfo("same", 1, "A", "10:00:00", "10:01:00"), new StopTimeInfo("same", 2, "B", "10:00:00", "10:01:00")),
                        "nonmono", List.of(new StopTimeInfo("nonmono", 1, "A", "11:00:00", "11:00:00"), new StopTimeInfo("nonmono", 2, "B", "10:59:00", "10:59:00")),
                        "missing", List.of(new StopTimeInfo("missing", 1, "A", null, null))),
                Map.of(),
                0,
                10,
                true,
                true,
                false,
                true,
                false,
                false,
                false,
                false);

        var result = new ScheduledTripTimeValidationService(new GtfsTimeParser(), 6.0d).analyze("feed", data);

        assertEquals(6, result.summary().get("trip_count"));
        assertTrue(result.tripRows().stream().anyMatch(row -> "VALID_SCHEDULE_DURATION".equals(row.get("scheduled_time_qc_status"))));
        assertTrue(result.tripRows().stream().anyMatch(row -> "ZERO_DURATION".equals(row.get("scheduled_time_qc_status"))));
        assertTrue(result.tripRows().stream().anyMatch(row -> "NON_MONOTONIC_TIME".equals(row.get("scheduled_time_qc_status"))));
        assertTrue(result.tripRows().stream().anyMatch(row -> "MISSING_FIRST_TIME".equals(row.get("scheduled_time_qc_status"))));
    }
}
