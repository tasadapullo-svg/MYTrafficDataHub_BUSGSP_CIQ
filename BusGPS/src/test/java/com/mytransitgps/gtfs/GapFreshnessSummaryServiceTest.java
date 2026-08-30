package com.mytransitgps.gtfs;

import java.util.List;
import java.util.Map;

import com.mytransitgps.gtfs.service.GapFreshnessSummaryService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GapFreshnessSummaryServiceTest {

    @Test
    void shouldSummarizePercentilesAndBuckets() {
        GapFreshnessSummaryService service = new GapFreshnessSummaryService();
        Map<String, Object> summary = service.summarize("feed", List.of(10L, 20L, 30L, 400L), 1L, List.of(60L, 180L, 301L, 900L));

        @SuppressWarnings("unchecked")
        Map<String, Object> freshness = (Map<String, Object>) summary.get("freshness");
        @SuppressWarnings("unchecked")
        Map<String, Object> gap = (Map<String, Object>) summary.get("gap");

        assertEquals(20L, freshness.get("p50"));
        assertEquals(400L, freshness.get("p95"));
        assertEquals(1L, freshness.get("future_count"));
        assertEquals(180L, gap.get("p50"));
        assertEquals(2L, gap.get("gt_180_count"));
    }
}
