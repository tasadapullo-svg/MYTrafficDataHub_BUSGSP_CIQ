package com.mytransitgps.ciqmanual;

import com.mytransitgps.modules.ciq.collector.CiqRoadOpeningsCollector;
import com.mytransitgps.platform.collection.CollectorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class Api08LiveCollectorTest extends CiqLiveTestSupport {
    @Autowired CiqRoadOpeningsCollector collector;

    @Test
    void liveApiAndDatabaseChain() {
        execute(collector, CollectorCode.CIQ_ROAD_OPENINGS,
                "SELECT COUNT(*) FROM lta.road_opening_event WHERE last_run_uid=?");
    }
}
