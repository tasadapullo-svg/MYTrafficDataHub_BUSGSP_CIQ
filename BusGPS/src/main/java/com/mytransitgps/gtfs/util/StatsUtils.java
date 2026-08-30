package com.mytransitgps.gtfs.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 统计计算工具，提供百分位数等报告指标的统一实现。
 */
public final class StatsUtils {

    private StatsUtils() {
    }

    public static Long percentileLong(List<Long> values, double percentile) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<Long> sorted = new ArrayList<>(values);
        sorted.sort(Comparator.naturalOrder());
        int index = percentileIndex(sorted.size(), percentile);
        return sorted.get(index);
    }

    public static Double percentileDouble(List<Double> values, double percentile) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<Double> sorted = new ArrayList<>(values);
        sorted.sort(Comparator.naturalOrder());
        int index = percentileIndex(sorted.size(), percentile);
        return sorted.get(index);
    }

    public static Long minLong(List<Long> values) {
        return values == null || values.isEmpty() ? null : values.stream().min(Long::compareTo).orElse(null);
    }

    public static Long maxLong(List<Long> values) {
        return values == null || values.isEmpty() ? null : values.stream().max(Long::compareTo).orElse(null);
    }

    public static Double minDouble(List<Double> values) {
        return values == null || values.isEmpty() ? null : values.stream().min(Double::compareTo).orElse(null);
    }

    public static Double maxDouble(List<Double> values) {
        return values == null || values.isEmpty() ? null : values.stream().max(Double::compareTo).orElse(null);
    }

    private static int percentileIndex(int size, double percentile) {
        double clamped = Math.max(0.0d, Math.min(100.0d, percentile));
        return (int) Math.ceil((clamped / 100.0d) * size) - 1 < 0
                ? 0
                : Math.min(size - 1, (int) Math.ceil((clamped / 100.0d) * size) - 1);
    }
}
