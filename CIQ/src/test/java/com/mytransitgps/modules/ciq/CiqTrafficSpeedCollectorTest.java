package com.mytransitgps.modules.ciq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mytransitgps.modules.ciq.client.LtaTrafficSpeedClient;
import com.mytransitgps.modules.ciq.collector.CiqTrafficSpeedCollector;
import com.mytransitgps.modules.ciq.config.CiqProperties;
import com.mytransitgps.modules.ciq.storage.CiqJsonStorageService;
import com.mytransitgps.persistence.config.MyTransitGpsDatabaseProperties;
import com.mytransitgps.persistence.service.WorkspaceRootResolver;
import com.mytransitgps.platform.collection.CollectionContext;
import com.mytransitgps.platform.collection.CollectionStatus;
import com.mytransitgps.platform.collection.CollectorCode;
import com.mytransitgps.platform.collection.ModuleCode;
import com.mytransitgps.platform.http.HttpRequestResult;
import com.mytransitgps.platform.http.TrafficHttpClient;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** 验证 API01 Raw-only 模式仍能完整分页并按标准目录保存科研原始 JSON。 */
class CiqTrafficSpeedCollectorTest {
    @TempDir
    Path tempDir;

    @Test
    void collectsPagedRawJsonSuccessfullyWhenDatabaseWriteIsDisabled() throws Exception {
        Files.createDirectories(tempDir.resolve("BusGPS"));
        Files.createDirectories(tempDir.resolve("CIQ"));
        Files.createDirectories(tempDir.resolve("logs"));

        MyTransitGpsDatabaseProperties workspaceProperties = new MyTransitGpsDatabaseProperties();
        workspaceProperties.setWorkspaceRoot(tempDir.toString());
        WorkspaceRootResolver resolver = new WorkspaceRootResolver(workspaceProperties);

        CiqProperties properties = new CiqProperties();
        properties.setEnabled(true);
        properties.getLta().setBaseUrl("https://example.test/lta");
        properties.getLta().setAccountKey("test-key-not-a-real-secret");
        properties.getCollectors().getTrafficSpeed().setEnabled(true);
        properties.getCollectors().getTrafficSpeed().setEndpoint("/TrafficSpeedBands");
        properties.getCollectors().getTrafficSpeed().setPageSize(2);
        properties.getCollectors().getTrafficSpeed().setMaxPages(10);
        properties.getStorage().setRootDirectory("CIQ");
        properties.getPersistence().setDatabaseWriteEnabled(false);

        AtomicInteger calls = new AtomicInteger();
        TrafficHttpClient fakeHttp = (uri, headers, connectTimeout, requestTimeout) -> {
            int call = calls.incrementAndGet();
            String json = call == 1
                    ? "{\"value\":[{\"LinkID\":\"1\"},{\"LinkID\":\"2\"}]}"
                    : "{\"value\":[{\"LinkID\":\"3\"}]}";
            Instant now = Instant.parse("2026-08-29T12:00:00Z").plusSeconds(call);
            return new HttpRequestResult(uri, uri, 200, json.getBytes(StandardCharsets.UTF_8), Map.of(),
                    now.minusMillis(10), now, 10, null, null);
        };

        LtaTrafficSpeedClient client = new LtaTrafficSpeedClient(fakeHttp, properties);
        CiqJsonStorageService storage = new CiqJsonStorageService(resolver, properties);
        CiqTrafficSpeedCollector collector = new CiqTrafficSpeedCollector(client, properties, storage, new ObjectMapper());
        CollectionContext context = CollectionContext.scheduled(ModuleCode.CIQ, CollectorCode.CIQ_TRAFFIC_SPEED,
                Instant.parse("2026-08-29T12:00:00Z"));

        var result = collector.collect(context);

        assertEquals(2, calls.get());
        assertEquals(2, result.requestCount());
        assertEquals(3, result.receivedCount());
        assertTrue(result.success());
        assertEquals(CollectionStatus.SUCCESS, result.status());
        assertEquals(null, result.errorCode());
        Path api01 = tempDir.resolve("CIQ/20260829/API01_TrafficSpeedBands");
        assertTrue(Files.isDirectory(api01));
        try (var files = Files.list(api01)) {
            assertEquals(2, files.filter(path -> path.getFileName().toString().endsWith(".json")).count());
        }
    }
}
