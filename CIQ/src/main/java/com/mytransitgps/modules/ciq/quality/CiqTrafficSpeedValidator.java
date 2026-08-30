package com.mytransitgps.modules.ciq.quality;

import com.mytransitgps.modules.ciq.domain.TrafficSpeedBandRecord;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * API01 TrafficSpeedBands 质量检查器。
 *
 * <p>该类只判断字段质量和页内重复 LinkID，不负责 PostGIS 空间范围判断，也不会把新加坡其他区域
 * 的合法 Link 误判为质量拒绝。</p>
 */
public class CiqTrafficSpeedValidator {
    private static final BigDecimal MIN_LON = BigDecimal.valueOf(-180);
    private static final BigDecimal MAX_LON = BigDecimal.valueOf(180);
    private static final BigDecimal MIN_LAT = BigDecimal.valueOf(-90);
    private static final BigDecimal MAX_LAT = BigDecimal.valueOf(90);

    /**
     * 对单页记录执行 QC，页内重复 LinkID 标记为 WARNING/DUPLICATE。
     */
    public List<TrafficSpeedValidationResult> validatePage(List<TrafficSpeedBandRecord> records) {
        Set<String> seenLinkIds = new HashSet<>();
        List<TrafficSpeedValidationResult> results = new ArrayList<>(records.size());
        for (TrafficSpeedBandRecord record : records) {
            List<String> issues = new ArrayList<>();
            boolean rejected = false;
            String linkId = record.linkId();
            if (linkId == null || linkId.isBlank()) {
                issues.add("LINK_ID_EMPTY");
                rejected = true;
            }
            if (record.speedBand() == null || record.speedBand() < 1 || record.speedBand() > 8) {
                issues.add("SPEED_BAND_INVALID");
                rejected = true;
            }
            if (record.minimumSpeed() != null && record.minimumSpeed() < 0) {
                issues.add("MINIMUM_SPEED_NEGATIVE");
                rejected = true;
            }
            if (record.minimumSpeed() != null && record.maximumSpeed() != null
                    && record.maximumSpeed() < record.minimumSpeed()) {
                issues.add("MAXIMUM_SPEED_LESS_THAN_MINIMUM");
                rejected = true;
            }
            if (!validLon(record.startLon()) || !validLon(record.endLon())
                    || !validLat(record.startLat()) || !validLat(record.endLat())) {
                issues.add("WGS84_INVALID");
                rejected = true;
            }
            if (isZeroZero(record.startLon(), record.startLat()) || isZeroZero(record.endLon(), record.endLat())) {
                issues.add("ZERO_ZERO_COORDINATE");
                rejected = true;
            }
            if (record.roadName() == null || record.roadName().isBlank()) {
                issues.add("ROAD_NAME_EMPTY");
            }
            if (record.roadCategory() == null || record.roadCategory() < 0) {
                issues.add("ROAD_CATEGORY_ABNORMAL");
            }
            boolean duplicate = linkId != null && !linkId.isBlank() && !seenLinkIds.add(linkId);
            if (duplicate) {
                issues.add("DUPLICATE_LINK_ID_IN_PAGE");
            }
            TrafficSpeedQualityStatus status = rejected
                    ? TrafficSpeedQualityStatus.REJECTED
                    : issues.isEmpty() ? TrafficSpeedQualityStatus.VALID : TrafficSpeedQualityStatus.WARNING;
            results.add(new TrafficSpeedValidationResult(record, status, List.copyOf(issues), duplicate));
        }
        return results;
    }

    private static boolean validLon(BigDecimal value) {
        return value != null && value.compareTo(MIN_LON) >= 0 && value.compareTo(MAX_LON) <= 0;
    }

    private static boolean validLat(BigDecimal value) {
        return value != null && value.compareTo(MIN_LAT) >= 0 && value.compareTo(MAX_LAT) <= 0;
    }

    private static boolean isZeroZero(BigDecimal lon, BigDecimal lat) {
        return lon != null && lat != null
                && BigDecimal.ZERO.compareTo(lon) == 0
                && BigDecimal.ZERO.compareTo(lat) == 0;
    }
}
