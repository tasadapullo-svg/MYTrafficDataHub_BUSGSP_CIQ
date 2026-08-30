package com.mytransitgps.ciqmanual;

import com.mytransitgps.modules.ciq.collector.CiqTrafficFlowCollector;
import com.mytransitgps.platform.collection.CollectorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class Api07LiveCollectorTest extends CiqLiveTestSupport {
    @Autowired CiqTrafficFlowCollector collector;

    @Test
    void liveApiAndDatabaseChain() {
        execute(collector, CollectorCode.CIQ_TRAFFIC_FLOW,
                "SELECT COUNT(*) FROM lta.traffic_flow_file WHERE run_uid=?");
    }
}
