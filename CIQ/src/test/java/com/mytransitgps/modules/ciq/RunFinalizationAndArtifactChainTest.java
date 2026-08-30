package com.mytransitgps.modules.ciq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mytransitgps.modules.ciq.client.LtaTrafficSpeedClient;
import com.mytransitgps.modules.ciq.collector.CiqTrafficSpeedCollector;
import com.mytransitgps.modules.ciq.config.CiqProperties;
import com.mytransitgps.modules.ciq.parser.TrafficSpeedBandsParser;
import com.mytransitgps.modules.ciq.persistence.CiqTrafficSpeedPersistenceService;
import com.mytransitgps.modules.ciq.quality.CiqTrafficSpeedValidator;
import com.mytransitgps.modules.ciq.storage.CiqApiCode;
import com.mytransitgps.modules.ciq.storage.CiqJsonStorageService;
import com.mytransitgps.modules.ciq.storage.CiqRawJsonArtifact;
import com.mytransitgps.platform.collection.CollectionContext;
import com.mytransitgps.platform.collection.CollectionStatus;
import com.mytransitgps.platform.collection.CollectorCode;
import com.mytransitgps.platform.collection.ModuleCode;
import com.mytransitgps.platform.http.HttpRequestResult;
import com.mytransitgps.platform.http.TrafficHttpClient;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/** 验证 API01 异常路径不会留下未关闭 collection_run，且 artifact 先于 Parser。 */
class RunFinalizationAndArtifactChainTest {
    @Test
    void artifactIsRegisteredBeforeParserFailureAndRunIsFinalized() throws Exception {
        CiqProperties properties = properties();
        byte[] body = "{\"value\":[]}".getBytes(StandardCharsets.UTF_8);
        TrafficHttpClient http = (uri, headers, connectTimeout, requestTimeout) ->
                new HttpRequestResult(uri, uri, 200, body, Map.of(), Instant.now(), Instant.now(), 10, null, null);
        LtaTrafficSpeedClient client = new LtaTrafficSpeedClient(http, properties);
        CiqJsonStorageService storage = mock(CiqJsonStorageService.class);
        CiqRawJsonArtifact artifact = new CiqRawJsonArtifact(CiqApiCode.API01, LocalDate.of(2026, 8, 30),
                1, Path.of("traffic_speed.json"), body.length, "a".repeat(64));
        when(storage.saveRawJson(eq(CiqApiCode.API01), any(LocalDate.class), any(UUID.class), eq(1),
                any(Instant.class), eq(body))).thenReturn(artifact);
        CiqTrafficSpeedPersistenceService persistence = mock(CiqTrafficSpeedPersistenceService.class);
        when(persistence.hasRequiredStudyAreas()).thenReturn(true);
        when(persistence.apiEndpointUid("API01")).thenReturn(UUID.randomUUID());
        TrafficSpeedBandsParser parser = mock(TrafficSpeedBandsParser.class);
        when(parser.parse(body)).thenThrow(new java.io.IOException("parser failed"));
        CiqTrafficSpeedCollector collector = new CiqTrafficSpeedCollector(client, properties, storage,
                new ObjectMapper(), parser, new CiqTrafficSpeedValidator(), persistence);

        var result = collector.collect(context());

        assertEquals(CollectionStatus.FAILED, result.status());
        assertEquals("INVALID_JSON", result.errorCode());
        InOrder order = inOrder(persistence, parser);
        order.verify(persistence).registerArtifact(any(UUID.class), eq(artifact));
        order.verify(parser).parse(body);
        verify(persistence).finalizeRun(any(UUID.class), any(Instant.class), anyInt(), anyInt(), anyLong(),
                anyLong(), eq(false), eq(false), anyShort(), anyString());
    }

    private static CiqProperties properties() {
        CiqProperties properties = new CiqProperties();
        properties.setEnabled(true);
        properties.getLta().setBaseUrl("https://example.test/lta");
        properties.getLta().setAccountKey("configured");
        properties.getCollectors().getTrafficSpeed().setEndpoint("/TrafficSpeedBands");
        properties.getCollectors().getTrafficSpeed().setPageSize(500);
        properties.getCollectors().getTrafficSpeed().setMaxPages(1);
        properties.getPersistence().setDatabaseWriteEnabled(true);
        return properties;
    }

    private static CollectionContext context() {
        return new CollectionContext(UUID.randomUUID(), Instant.now(), null,
                ModuleCode.CIQ, CollectorCode.CIQ_TRAFFIC_SPEED, UUID.randomUUID().toString(), true, Map.of());
    }
}
