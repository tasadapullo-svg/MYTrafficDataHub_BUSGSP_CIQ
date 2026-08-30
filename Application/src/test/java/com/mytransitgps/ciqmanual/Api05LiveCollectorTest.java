package com.mytransitgps.ciqmanual;

import com.mytransitgps.modules.ciq.collector.CiqFaultyTrafficLightsCollector;
import com.mytransitgps.platform.collection.CollectorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class Api05LiveCollectorTest extends CiqLiveTestSupport {
    @Autowired CiqFaultyTrafficLightsCollector collector;

    @Test
    void liveApiAndDatabaseChain() {
        execute(collector, CollectorCode.CIQ_FAULTY_TRAFFIC_LIGHTS,
                "SELECT COUNT(*) FROM lta.faulty_traffic_light_event WHERE last_run_uid=?");
    }
}
