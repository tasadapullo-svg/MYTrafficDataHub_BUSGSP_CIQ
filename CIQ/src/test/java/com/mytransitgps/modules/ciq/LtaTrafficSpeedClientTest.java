package com.mytransitgps.modules.ciq;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mytransitgps.modules.ciq.client.LtaTrafficSpeedClient;
import com.mytransitgps.modules.ciq.config.CiqProperties;
import com.mytransitgps.platform.http.TrafficHttpClient;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class LtaTrafficSpeedClientTest {
    @Test
    void missingAccountKeyMustPreventHttpRequest() {
        AtomicBoolean called = new AtomicBoolean();
        TrafficHttpClient httpClient = (uri, headers, connectTimeout, requestTimeout) -> {
            called.set(true);
            throw new AssertionError("HTTP不应被调用");
        };

        LtaTrafficSpeedClient client = new LtaTrafficSpeedClient(httpClient, new CiqProperties());

        assertThrows(IllegalStateException.class, client::fetchRaw);
        org.junit.jupiter.api.Assertions.assertFalse(called.get());
    }
}
