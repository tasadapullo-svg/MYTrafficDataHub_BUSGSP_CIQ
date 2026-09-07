package com.mytransitgps.modules.ciqbus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mytransitgps.modules.ciqbus.client.LtaCrossBorderBusClient;
import com.mytransitgps.modules.ciqbus.config.CiqBusProperties;
import com.mytransitgps.platform.http.HttpRequestResult;
import com.mytransitgps.platform.http.TrafficHttpClient;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class LtaCrossBorderBusClientTest {
    @Test
    void missingAccountKeyMustPreventHttpRequest() {
        AtomicBoolean called = new AtomicBoolean();
        TrafficHttpClient httpClient = (uri, headers, connectTimeout, requestTimeout) -> {
            called.set(true);
            throw new AssertionError("LTA_ACCOUNT_KEY 缺失时不应发出请求");
        };

        LtaCrossBorderBusClient client = new LtaCrossBorderBusClient(httpClient, new CiqBusProperties());

        assertThrows(IllegalStateException.class, () -> client.fetchBusArrival("46219"));
        assertTrue(!called.get());
    }

    @Test
    void configuredAccountKeyIsSentOnlyInAccountKeyHeader() {
        CiqBusProperties properties = new CiqBusProperties();
        properties.getLta().setAccountKey("fake-test-key");
        AtomicBoolean called = new AtomicBoolean();
        TrafficHttpClient httpClient = (uri, headers, connectTimeout, requestTimeout) -> {
            called.set(true);
            assertEquals("fake-test-key", headers.get("AccountKey"));
            assertEquals("application/json", headers.get("Accept"));
            assertTrue(uri.toString().contains("/v3/BusArrival?BusStopCode=46219"));
            return new HttpRequestResult(uri, uri, 200,
                    "{\"Services\":[]}".getBytes(StandardCharsets.UTF_8),
                    Map.of("content-type", List.of("application/json")),
                    Instant.now(), Instant.now(), 1, null, null);
        };

        LtaCrossBorderBusClient client = new LtaCrossBorderBusClient(httpClient, properties);

        assertEquals(200, client.fetchBusArrival("46219").statusCode());
        assertTrue(called.get());
    }
}
