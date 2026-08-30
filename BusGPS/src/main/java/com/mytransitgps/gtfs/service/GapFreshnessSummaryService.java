package com.mytransitgps.gtfs.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mytransitgps.gtfs.util.StatsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 车辆上报间隔和数据新鲜度统计服务，用于识别延迟、停更和时间异常。
 */
public class GapFreshnessSummaryService {

    private static final Logger log = LoggerFactory.getLogger(GapFreshnessSummaryService.class);

    public Map<String, Object> summarize(String feedId, List<Long> freshnessSamples, long futureCount, List<Long> gapSamples) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("feed_id", feedId);
        root.put("freshness", summarizeFreshness(freshnessSamples, futureCount));
        root.put("gap", summarizeGap(gapSamples));
        log.info("车辆新鲜度与间隔汇总完成，feedId={}，freshnessSamples={}，futureCount={}，gapSamples={}",
                feedId, freshnessSamples.size(), futureCount, gapSamples.size());
        if (futureCount > 0 || gapSamples.stream().anyMatch(value -> value > 600L)) {
            log.warn("车辆时间质量存在异常，feedId={}，futureCount={}，longGapOver600Count={}",
                    feedId, futureCount, gapSamples.stream().filter(value -> value > 600L).count());
        }
        return root;
    }

    private Map<String, Object> summarizeFreshness(List<Long> samples, long futureCount) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("count", samples.size());
        map.put("p50", StatsUtils.percentileLong(samples, 50));
        map.put("p90", StatsUtils.percentileLong(samples, 90));
        map.put("p95", StatsUtils.percentileLong(samples, 95));
        map.put("max", StatsUtils.maxLong(samples));
        map.put("lte_120_count", samples.stream().filter(value -> value <= 120L).count());
        map.put("sec_121_300_count", samples.stream().filter(value -> value >= 121L && value <= 300L).count());
        map.put("sec_301_600_count", samples.stream().filter(value -> value >= 301L && value <= 600L).count());
        map.put("gt_600_count", samples.stream().filter(value -> value > 600L).count());
        map.put("future_count", futureCount);
        return map;
    }

    private Map<String, Object> summarizeGap(List<Long> samples) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("count", samples.size());
        map.put("p50", StatsUtils.percentileLong(samples, 50));
        map.put("p90", StatsUtils.percentileLong(samples, 90));
        map.put("p95", StatsUtils.percentileLong(samples, 95));
        map.put("max", StatsUtils.maxLong(samples));
        map.put("gt_180_count", samples.stream().filter(value -> value > 180L).count());
        map.put("gt_300_count", samples.stream().filter(value -> value > 300L).count());
        map.put("gt_600_count", samples.stream().filter(value -> value > 600L).count());
        return map;
    }
}
