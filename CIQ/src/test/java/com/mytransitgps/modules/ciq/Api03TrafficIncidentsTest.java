package com.mytransitgps.modules.ciq;
import static org.junit.jupiter.api.Assertions.*;import com.fasterxml.jackson.databind.ObjectMapper;import com.mytransitgps.modules.ciq.parser.TrafficIncidentsParser;import com.mytransitgps.modules.ciq.quality.TrafficIncidentsValidator;import java.nio.charset.StandardCharsets;import org.junit.jupiter.api.Test;
/** 单接口 Parser + Validator 节点测试。 */
class Api03TrafficIncidentsTest {@Test void parsesAndValidatesFixture(){var rows=new TrafficIncidentsParser(new ObjectMapper()).parse("{\"value\":[{\"Type\":\"Roadwork\",\"Latitude\":1.3774,\"Longitude\":103.7307,\"Message\":\"Road Works on KJE\"}]}".getBytes(StandardCharsets.UTF_8));assertFalse(rows.isEmpty());assertTrue(new TrafficIncidentsValidator().isValid(rows.get(0)));}}
