package com.mytransitgps.modules.ciq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.mytransitgps.modules.ciq.config.CiqProperties;
import com.mytransitgps.modules.ciq.domain.TrafficSpeedBandRecord;
import com.mytransitgps.modules.ciq.persistence.CiqTrafficSpeedPersistenceService;
import com.mytransitgps.modules.ciq.persistence.TrafficSpeedPageData;
import com.mytransitgps.modules.ciq.persistence.TrafficSpeedPersistenceResult;
import com.mytransitgps.modules.ciq.quality.TrafficSpeedQualityStatus;
import com.mytransitgps.modules.ciq.quality.TrafficSpeedValidationResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

/** 验证 API01 单页持久化统计可正确汇总到完整 snapshot。 */
class TrafficSpeedPersistenceTest {
    @Test
    void combinesPerPagePersistenceCounters() {
        TrafficSpeedPersistenceResult first = new TrafficSpeedPersistenceResult(1, 2, 3, 4, 5, 6, 7);
        TrafficSpeedPersistenceResult second = new TrafficSpeedPersistenceResult(10, 20, 30, 40, 50, 60, 70);

        TrafficSpeedPersistenceResult total = first.plus(second);

        assertEquals(11, total.newLinkCount());
        assertEquals(22, total.updatedLinkCount());
        assertEquals(33, total.scopeRows());
        assertEquals(44, total.observationsInserted());
        assertEquals(55, total.observationDuplicates());
        assertEquals(66, total.outOfScopeCount());
        assertEquals(77, total.uniqueInScopeLinks());
    }

    @Test
    void fiveHundredCandidatesUseOneBatchSpatialQuery() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        CiqProperties properties = new CiqProperties();
        properties.getPersistence().setBatchSize(500);
        CiqTrafficSpeedPersistenceService service = new CiqTrafficSpeedPersistenceService(jdbcTemplate, properties);
        List<TrafficSpeedValidationResult> rows = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            TrafficSpeedBandRecord record = new TrafficSpeedBandRecord("L" + i, "BKE", (short) 1, (short) 4,
                    (short) 30, (short) 39, bd("103.7"), bd("1.4"), bd("103.8"), bd("1.5"));
            rows.add(new TrafficSpeedValidationResult(record, TrafficSpeedQualityStatus.VALID, List.of(), false));
        }

        TrafficSpeedPersistenceResult result = service.persistPage(java.util.UUID.randomUUID(), Instant.now(),
                new TrafficSpeedPageData(1, 0, rows.stream().map(TrafficSpeedValidationResult::record).toList(), rows));

        assertEquals(500, result.outOfScopeCount());
        verify(jdbcTemplate, times(1)).query(anyString(), any(RowCallbackHandler.class), any(Object[].class));
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
