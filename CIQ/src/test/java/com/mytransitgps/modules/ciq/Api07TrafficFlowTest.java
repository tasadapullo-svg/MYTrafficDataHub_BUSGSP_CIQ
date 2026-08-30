package com.mytransitgps.modules.ciq;
import static org.junit.jupiter.api.Assertions.*;import com.fasterxml.jackson.databind.ObjectMapper;import com.mytransitgps.modules.ciq.parser.TrafficFlowParser;import com.mytransitgps.modules.ciq.quality.TrafficFlowValidator;import java.nio.charset.StandardCharsets;import org.junit.jupiter.api.Test;
/** 单接口 Parser + Validator 节点测试。 */
class Api07TrafficFlowTest {@Test void parsesAndValidatesFixture(){var rows=new TrafficFlowParser(new ObjectMapper()).parse("{\"value\":[{\"Link\":\"https://example.test/traffic-flow.csv\"}]}".getBytes(StandardCharsets.UTF_8));assertFalse(rows.isEmpty());assertTrue(new TrafficFlowValidator().isValid(rows.get(0)));}}
