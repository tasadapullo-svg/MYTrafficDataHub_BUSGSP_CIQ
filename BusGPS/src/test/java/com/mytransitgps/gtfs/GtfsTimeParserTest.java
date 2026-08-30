package com.mytransitgps.gtfs;

import com.mytransitgps.gtfs.util.GtfsTimeParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GtfsTimeParserTest {

    private final GtfsTimeParser parser = new GtfsTimeParser();

    @Test
    void shouldParseGtfsTimesBeyondTwentyFourHours() {
        assertEquals(0, parser.parseToSeconds("00:00:00"));
        assertEquals(86399, parser.parseToSeconds("23:59:59"));
        assertEquals(86400, parser.parseToSeconds("24:00:00"));
        assertEquals(91815, parser.parseToSeconds("25:30:15"));
        assertEquals(108000, parser.parseToSeconds("30:00:00"));
    }
}
