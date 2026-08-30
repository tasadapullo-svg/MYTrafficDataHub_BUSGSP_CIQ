package com.mytransitgps.modules.ciq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mytransitgps.modules.ciq.domain.TrafficSpeedBandRecord;
import com.mytransitgps.modules.ciq.quality.CiqTrafficSpeedValidator;
import com.mytransitgps.modules.ciq.quality.TrafficSpeedQualityStatus;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 验证 API01 QC 与空间范围判断解耦。 */
class CiqTrafficSpeedValidatorTest {
    private final CiqTrafficSpeedValidator validator = new CiqTrafficSpeedValidator();

    @Test
    void rejectsHardFieldErrorsAndWarnsForSoftIssuesAndDuplicates() {
        TrafficSpeedBandRecord valid = record("A", "BKE", (short) 4, "103.7", "1.4", "103.8", "1.5");
        TrafficSpeedBandRecord warning = new TrafficSpeedBandRecord("B", "", (short) -1, (short) 4,
                (short) 30, (short) 39, bd("103.7"), bd("1.4"), bd("103.8"), bd("1.5"));
        TrafficSpeedBandRecord duplicate = record("A", "BKE", (short) 4, "103.7", "1.4", "103.8", "1.5");
        TrafficSpeedBandRecord rejected = record("", "BKE", (short) 9, "0", "0", "181", "1.5");

        var results = validator.validatePage(List.of(valid, warning, duplicate, rejected));

        assertEquals(TrafficSpeedQualityStatus.VALID, results.get(0).status());
        assertEquals(TrafficSpeedQualityStatus.WARNING, results.get(1).status());
        assertTrue(results.get(1).issues().contains("ROAD_NAME_EMPTY"));
        assertEquals(TrafficSpeedQualityStatus.WARNING, results.get(2).status());
        assertTrue(results.get(2).duplicateInPage());
        assertEquals(TrafficSpeedQualityStatus.REJECTED, results.get(3).status());
        assertTrue(results.get(3).issues().contains("LINK_ID_EMPTY"));
        assertTrue(results.get(3).issues().contains("SPEED_BAND_INVALID"));
        assertTrue(results.get(3).issues().contains("WGS84_INVALID"));
        assertTrue(results.get(3).issues().contains("ZERO_ZERO_COORDINATE"));
    }

    private static TrafficSpeedBandRecord record(String linkId, String roadName, short speedBand,
                                                 String startLon, String startLat, String endLon, String endLat) {
        return new TrafficSpeedBandRecord(linkId, roadName, (short) 1, speedBand, (short) 30, (short) 39,
                bd(startLon), bd(startLat), bd(endLon), bd(endLat));
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}

