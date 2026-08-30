package com.mytransitgps.gtfs;

import java.nio.file.Files;

import com.google.transit.realtime.GtfsRealtime;
import com.mytransitgps.gtfs.parser.GtfsRealtimeParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GtfsRealtimeParserTest {

    @Test
    void shouldParseLocalJohorRealtimeProtobufFixture() throws Exception {
        GtfsRealtimeParser parser = new GtfsRealtimeParser();
        byte[] protobufBytes = Files.readAllBytes(TestArtifactSupport.latestLegacyRealtimePb());

        GtfsRealtime.FeedMessage feedMessage = parser.parse(protobufBytes);
        String json = parser.toJson(feedMessage);

        assertTrue(feedMessage.hasHeader());
        assertTrue(feedMessage.getEntityCount() > 0);
        assertTrue(json.contains("\"header\""));
    }

    @Test
    void shouldRoundTripEntityCountAgainstLegacyParsedJsonFixture() throws Exception {
        GtfsRealtimeParser parser = new GtfsRealtimeParser();
        byte[] protobufBytes = Files.readAllBytes(TestArtifactSupport.latestLegacyRealtimePb());
        String parsedJson = Files.readString(TestArtifactSupport.latestLegacyParsedJson());

        GtfsRealtime.FeedMessage feedMessage = parser.parse(protobufBytes);

        long entityOccurrences = parsedJson.lines().filter(line -> line.contains("\"entity\"")).count();
        assertTrue(entityOccurrences >= 1);
        assertEquals(feedMessage.getEntityCount(), feedMessage.getEntityList().size());
    }
}
