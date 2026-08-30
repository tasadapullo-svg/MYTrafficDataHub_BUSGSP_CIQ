package com.mytransitgps.gtfs.service;

import java.util.Set;

/**
 * 观测可用性策略，根据位置、时间和 QC 状态判定分析及空间分析资格。
 */
public class ObservationEligibilityPolicy {

    public Eligibility evaluate(Set<String> qcFlags, String positionQcStatus, String gpsJumpStatus) {
        // eligibility 仅生成分析标签，不删除任何异常记录或原始字段。
        boolean spatialEligible = "VALID_POSITION".equals(positionQcStatus);
        if ("OUT_OF_BOUNDS".equals(positionQcStatus)) {
            spatialEligible = false;
        }

        boolean analysisEligible = !containsAny(qcFlags, Set.of(
                "MISSING_POSITION",
                "INVALID_WGS84",
                "ZERO_ZERO_POSITION",
                "FUTURE_TIMESTAMP",
                "OUT_OF_BOUNDS",
                "GPS_JUMP"));

        if ("GPS_JUMP".equals(gpsJumpStatus)) {
            analysisEligible = false;
        }

        return new Eligibility(analysisEligible, spatialEligible);
    }

    private boolean containsAny(Set<String> flags, Set<String> disqualifyingFlags) {
        for (String flag : disqualifyingFlags) {
            if (flags.contains(flag)) {
                return true;
            }
        }
        return false;
    }

    /** 一条观测的普通分析与空间分析可用性标签。 */
    public record Eligibility(
            boolean analysisEligible,
            boolean spatialEligible) {
    }
}
