package com.mytransitgps.ciqmanual;

import com.mytransitgps.modules.ciq.collector.CiqEstimatedTravelTimesCollector;
import com.mytransitgps.platform.collection.CollectorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class Api02LiveCollectorTest extends CiqLiveTestSupport {
    @Autowired CiqEstimatedTravelTimesCollector collector;

    @Test
    void liveApiAndDatabaseChain() {
        execute(collector, CollectorCode.CIQ_EST_TRAVEL_TIMES,
                "SELECT COUNT(*) FROM lta.travel_time_observation WHERE run_uid=?");
    }
}
