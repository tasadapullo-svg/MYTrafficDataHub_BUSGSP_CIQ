package com.mytransitgps.ciqmanual;

import com.mytransitgps.modules.ciq.collector.CiqRoadWorksCollector;
import com.mytransitgps.platform.collection.CollectorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class Api06LiveCollectorTest extends CiqLiveTestSupport {
    @Autowired CiqRoadWorksCollector collector;

    @Test
    void liveApiAndDatabaseChain() {
        execute(collector, CollectorCode.CIQ_ROAD_WORKS,
                "SELECT COUNT(*) FROM lta.road_work_event WHERE last_run_uid=?");
    }
}
