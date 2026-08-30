package com.mytransitgps.gtfs.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Protobuf 字段存在性审计结果，用于区分缺失字段与默认值。
 */
public record FieldPresenceAudit(
        String feedId,
        int vehicleCount,
        Map<String, FieldPresenceCount> fields) {

    /** 单个 Protobuf 字段的出现次数与车辆总数。 */
    public record FieldPresenceCount(int presentCount, int vehicleCount) {
        public double percentage() {
            if (vehicleCount == 0) {
                return 0.0d;
            }
            return (presentCount * 100.0d) / vehicleCount;
        }
    }

    /** 按实体累加字段出现次数并构造不可变审计结果。 */
    public static class Builder {
        private final String feedId;
        private final int vehicleCount;
        private final Map<String, FieldPresenceCount> fields = new LinkedHashMap<>();

        public Builder(String feedId, int vehicleCount) {
            this.feedId = feedId;
            this.vehicleCount = vehicleCount;
        }

        public Builder put(String fieldName, int presentCount) {
            fields.put(fieldName, new FieldPresenceCount(presentCount, vehicleCount));
            return this;
        }

        public FieldPresenceAudit build() {
            return new FieldPresenceAudit(feedId, vehicleCount, Map.copyOf(fields));
        }
    }
}
