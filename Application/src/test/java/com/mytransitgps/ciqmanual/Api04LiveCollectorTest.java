package com.mytransitgps.ciqmanual;

import com.mytransitgps.modules.ciq.collector.CiqVmsCollector;
import com.mytransitgps.platform.collection.CollectorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class Api04LiveCollectorTest extends CiqLiveTestSupport {
    @Autowired CiqVmsCollector collector;

    @Test
    void liveApiAndDatabaseChain() {
        execute(collector, CollectorCode.CIQ_VMS,
                "SELECT COUNT(*) FROM lta.vms_message_state WHERE last_run_uid=?");
    }
}
