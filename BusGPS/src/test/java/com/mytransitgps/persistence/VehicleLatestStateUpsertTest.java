package com.mytransitgps.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.mytransitgps.persistence.mapper.JbVehicleLatestStateMapper;
import org.apache.ibatis.annotations.Insert;
import org.junit.jupiter.api.Test;

class VehicleLatestStateUpsertTest {
    @Test void upsertUsesLastSeenTimeAndNotVehicleTime() throws Exception {
        String sql = String.join(" ", JbVehicleLatestStateMapper.class.getMethod("upsert", com.mytransitgps.persistence.entity.JbVehicleLatestStateEntity.class).getAnnotation(Insert.class).value());
        assertThat(sql).contains("ON CONFLICT (feed_uid, vehicle_id)").contains("EXCLUDED.last_seen_time >= jb.vehicle_latest_state.last_seen_time");
        assertThat(sql).doesNotContain("EXCLUDED.vehicle_time >=");
    }
}
