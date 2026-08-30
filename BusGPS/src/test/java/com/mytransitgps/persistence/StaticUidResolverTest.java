package com.mytransitgps.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import com.mytransitgps.persistence.entity.JbStaticRouteEntity;
import com.mytransitgps.persistence.mapper.JbStaticRouteMapper;
import com.mytransitgps.persistence.mapper.JbStaticShapeMapper;
import com.mytransitgps.persistence.mapper.JbStaticStopMapper;
import com.mytransitgps.persistence.mapper.JbStaticTripMapper;
import com.mytransitgps.persistence.mapper.JbStaticVersionMapper;
import com.mytransitgps.persistence.service.StaticUidResolver;
import org.junit.jupiter.api.Test;

class StaticUidResolverTest {
    @Test void dynamicallyLoadsSourceIdToUuid() {
        var versions = mock(JbStaticVersionMapper.class); var routes = mock(JbStaticRouteMapper.class);
        var trips = mock(JbStaticTripMapper.class); var stops = mock(JbStaticStopMapper.class); var shapes = mock(JbStaticShapeMapper.class);
        UUID routeUid = UUID.randomUUID(); var route = new JbStaticRouteEntity(); route.uid = routeUid; route.routeId = "R1";
        when(routes.selectList(any())).thenReturn(List.of(route)); when(trips.selectList(any())).thenReturn(List.of());
        when(stops.selectList(any())).thenReturn(List.of()); when(shapes.selectList(any())).thenReturn(List.of());
        assertThat(new StaticUidResolver(versions, routes, trips, stops, shapes).load(UUID.randomUUID(), "sha").routeUids()).containsEntry("R1", routeUid);
    }
}
