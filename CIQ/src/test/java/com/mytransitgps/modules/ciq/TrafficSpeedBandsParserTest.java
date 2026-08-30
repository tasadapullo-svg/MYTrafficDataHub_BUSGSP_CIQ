package com.mytransitgps.modules.ciq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mytransitgps.modules.ciq.parser.TrafficSpeedBandsParser;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** 验证 API01 TrafficSpeedBands Parser 对真实字段和宽松数字类型的兼容性。 */
class TrafficSpeedBandsParserTest {
    @Test
    void parsesStringNumbersNumberNodesNullsAndUnknownFields() throws Exception {
        byte[] body = getClass().getResourceAsStream("/fixtures/api01/traffic_speed_bands_fixture.json").readAllBytes();
        var records = new TrafficSpeedBandsParser(new ObjectMapper()).parse(body);

        assertEquals(2, records.size());
        assertEquals("103000000", records.get(0).linkId());
        assertEquals(Short.valueOf((short) 1), records.get(0).roadCategory());
        assertEquals(Short.valueOf((short) 4), records.get(0).speedBand());
        assertEquals(new BigDecimal("103.7701000000000000"), records.get(0).startLon());
        assertNull(records.get(1).roadName());
        assertEquals(Short.valueOf((short) 5), records.get(1).speedBand());
        assertEquals(new BigDecimal("103.7721"), records.get(1).startLon());
    }
}

