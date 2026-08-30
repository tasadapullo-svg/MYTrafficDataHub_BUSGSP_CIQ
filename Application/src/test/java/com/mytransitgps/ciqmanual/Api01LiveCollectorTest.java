package com.mytransitgps.ciqmanual;

import com.mytransitgps.modules.ciq.collector.CiqTrafficSpeedCollector;
import com.mytransitgps.platform.collection.CollectorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class Api01LiveCollectorTest extends CiqLiveTestSupport {
    @Autowired CiqTrafficSpeedCollector collector;

    @Test
    void liveApiAndDatabaseChain() {
        execute(collector, CollectorCode.CIQ_TRAFFIC_SPEED,
                "SELECT COUNT(*) FROM lta.traffic_speed_observation WHERE run_uid=?");
    }
}
