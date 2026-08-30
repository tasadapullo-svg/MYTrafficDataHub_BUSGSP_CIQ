package com.mytransitgps.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mytransitgps.persistence.model.StaticReferenceIndex;
import com.mytransitgps.persistence.service.VehicleObservationDatabaseMapper;
import org.junit.jupiter.api.Test;

class GtfsTimestampDatabaseMapperTest {
    @Test void mapsRawUnixSecondWithoutTimezoneShift() throws Exception {
        var root = new ObjectMapper().readTree("{\"snapshot\":{\"ingest_timestamp_utc\":\"2026-08-27T16:00:10Z\"},\"vehicles\":[{\"vehicle_id\":\"v1\",\"vehicle_timestamp\":1787846410,\"source_speed_present\":false,\"duplicate_observation\":false,\"qc_flags\":[],\"realtime_entity\":{}}]}");
        var refs = new StaticReferenceIndex(UUID.randomUUID(), "sha", Map.of(), Map.of(), Map.of(), Map.of());
        var row = new VehicleObservationDatabaseMapper().map(root, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), refs).observations().get(0);
        assertThat(row.vehicleTimestampRaw).isEqualTo(1787846410L);
        assertThat(row.vehicleTime).isEqualTo(Instant.ofEpochSecond(1787846410L));
    }
}
