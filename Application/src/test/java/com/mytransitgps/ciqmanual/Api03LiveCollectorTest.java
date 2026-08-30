package com.mytransitgps.ciqmanual;

import com.mytransitgps.modules.ciq.collector.CiqTrafficIncidentsCollector;
import com.mytransitgps.platform.collection.CollectorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class Api03LiveCollectorTest extends CiqLiveTestSupport {
    @Autowired CiqTrafficIncidentsCollector collector;

    @Test
    void liveApiAndDatabaseChain() {
        execute(collector, CollectorCode.CIQ_TRAFFIC_INCIDENTS,
                "SELECT COUNT(*) FROM lta.traffic_incident_event WHERE last_run_uid=?");
    }
}
