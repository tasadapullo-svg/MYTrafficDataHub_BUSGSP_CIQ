package com.mytransitgps.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mytransitgps.platform.http.JdkTrafficHttpClient;
import java.lang.reflect.Field;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** 验证平台 HTTP client 按连接超时复用底层 JDK HttpClient。 */
class JdkTrafficHttpClientReuseTest {
    @Test
    @SuppressWarnings("unchecked")
    void reusesHttpClientForSameConnectTimeout() throws Exception {
        JdkTrafficHttpClient client = new JdkTrafficHttpClient();
        Duration connectTimeout = Duration.ofMillis(20);
        client.get(URI.create("http://127.0.0.1:1/"), Map.of(), connectTimeout, Duration.ofMillis(20));
        client.get(URI.create("http://127.0.0.1:1/"), Map.of(), connectTimeout, Duration.ofMillis(20));

        Field field = JdkTrafficHttpClient.class.getDeclaredField("clients");
        field.setAccessible(true);
        Map<Duration, ?> clients = (Map<Duration, ?>) field.get(client);
        assertEquals(1, clients.size());
    }
}

