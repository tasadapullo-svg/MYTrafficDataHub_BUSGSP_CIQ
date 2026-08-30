package com.mytransitgps.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mytransitgps.persistence.config.MyTransitGpsDatabaseProperties;
import com.mytransitgps.persistence.entity.JbVehicleLatestStateEntity;
import com.mytransitgps.persistence.entity.JbVehicleObservationEntity;
import com.mytransitgps.persistence.mapper.JbRealtimeSnapshotMapper;
import com.mytransitgps.persistence.mapper.JbVehicleLatestStateMapper;
import com.mytransitgps.persistence.mapper.JbVehicleObservationMapper;
import com.mytransitgps.persistence.mapper.JbVehicleObservationQcMapper;
import com.mytransitgps.persistence.model.ObservationMappingResult;
import com.mytransitgps.persistence.model.SnapshotPersistenceContext;
import com.mytransitgps.persistence.model.StaticReferenceIndex;
import com.mytransitgps.persistence.service.RealtimePersistenceService;
import com.mytransitgps.persistence.service.VehicleObservationDatabaseMapper;
import org.junit.jupiter.api.Test;

class RealtimePersistenceServiceTest {
    @Test void batchesObservationAndUpsertsLatestWithinServiceCall() throws Exception {
        var props = new MyTransitGpsDatabaseProperties(); var dbMapper = mock(VehicleObservationDatabaseMapper.class);
        var snapshots = mock(JbRealtimeSnapshotMapper.class); var observations = mock(JbVehicleObservationMapper.class);
        var qc = mock(JbVehicleObservationQcMapper.class); var latest = mock(JbVehicleLatestStateMapper.class);
        var observation = new JbVehicleObservationEntity(); var state = new JbVehicleLatestStateEntity();
        when(dbMapper.map(any(), any(), any(), any(), any(), any())).thenReturn(new ObservationMappingResult(List.of(observation), List.of(), List.of(state)));
        var service = new RealtimePersistenceService(props, dbMapper, snapshots, observations, qc, latest);
        UUID id = UUID.randomUUID(); var refs = new StaticReferenceIndex(UUID.randomUUID(), "sha", Map.of(), Map.of(), Map.of(), Map.of());
        var result = service.persist(new ObjectMapper().readTree("{\"snapshot\":{}}"), new SnapshotPersistenceContext(id,id,id,id,null,false,refs));
        verify(snapshots).insert(any(com.mytransitgps.persistence.entity.JbRealtimeSnapshotEntity.class)); verify(observations).insert(org.mockito.ArgumentMatchers.<JbVehicleObservationEntity>anyList(), anyInt()); verify(latest).upsert(state);
        assertThat(result.observationCount()).isEqualTo(1);
    }
}
