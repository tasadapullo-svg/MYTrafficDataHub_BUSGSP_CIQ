package com.mytransitgps.modules.ciq;
import static org.junit.jupiter.api.Assertions.*;import com.fasterxml.jackson.databind.ObjectMapper;import com.mytransitgps.modules.ciq.parser.RoadOpeningsParser;import com.mytransitgps.modules.ciq.quality.RoadOpeningsValidator;import java.nio.charset.StandardCharsets;import org.junit.jupiter.api.Test;
/** 单接口 Parser + Validator 节点测试。 */
class Api08RoadOpeningsTest {@Test void parsesAndValidatesFixture(){var rows=new RoadOpeningsParser(new ObjectMapper()).parse("{\"value\":[{\"EventID\":\"RO1\",\"StartDate\":\"2026-09-01\",\"EndDate\":\"2026-09-02\",\"SvcDept\":\"LTA\",\"RoadName\":\"PIE\",\"Other\":\"test\"}]}".getBytes(StandardCharsets.UTF_8));assertFalse(rows.isEmpty());assertTrue(new RoadOpeningsValidator().isValid(rows.get(0)));}}
