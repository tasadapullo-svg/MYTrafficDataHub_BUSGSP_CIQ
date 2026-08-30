package com.mytransitgps.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mytransitgps.persistence.model.StaticReferenceIndex;
import com.mytransitgps.persistence.service.VehicleObservationDatabaseMapper;
import org.junit.jupiter.api.Test;

class VehicleObservationDatabaseMapperTest {
    @Test void preservesZeroSpeedZeroPositionJsonAndOccurrence() throws Exception {
        UUID route = UUID.randomUUID();
        var refs = new StaticReferenceIndex(UUID.randomUUID(), "sha", Map.of("R", route), Map.of(), Map.of(), Map.of());
        var root = new ObjectMapper().readTree("{\"snapshot\":{\"ingest_timestamp_utc\":\"2026-08-27T00:00:00Z\"},\"vehicles\":[{\"entity_id\":\"e\",\"vehicle_id\":\"v\",\"static_route_id\":\"R\",\"latitude\":0.0,\"longitude\":0.0,\"source_speed_raw\":0.0,\"source_speed_present\":true,\"zero_zero_position\":true,\"duplicate_observation\":false,\"qc_flags\":[\"ZERO_ZERO_POSITION\"],\"realtime_entity\":{\"id\":\"e\"}}]}");
        var mapped = new VehicleObservationDatabaseMapper().map(root, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), refs);
        assertThat(mapped.observations()).hasSize(1); var row = mapped.observations().get(0);
        assertThat(row.entitySequence).isZero(); assertThat(row.sourceSpeedRaw).isZero(); assertThat(row.geom).isNull();
        assertThat(row.staticRouteUid).isEqualTo(route); assertThat(row.realtimeEntity.path("id").asText()).isEqualTo("e");
        assertThat(mapped.qcRows()).extracting(q -> q.qcCode).containsExactly("ZERO_ZERO_POSITION");
    }
}
