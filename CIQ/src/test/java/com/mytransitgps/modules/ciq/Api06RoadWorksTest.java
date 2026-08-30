package com.mytransitgps.modules.ciq;
import static org.junit.jupiter.api.Assertions.*;import com.fasterxml.jackson.databind.ObjectMapper;import com.mytransitgps.modules.ciq.parser.RoadWorksParser;import com.mytransitgps.modules.ciq.quality.RoadWorksValidator;import java.nio.charset.StandardCharsets;import org.junit.jupiter.api.Test;
/** 单接口 Parser + Validator 节点测试。 */
class Api06RoadWorksTest {@Test void parsesAndValidatesFixture(){var rows=new RoadWorksParser(new ObjectMapper()).parse("{\"value\":[{\"EventID\":\"RW1\",\"StartDate\":\"2026-08-01\",\"EndDate\":\"2026-09-01\",\"SvcDept\":\"LTA\",\"RoadName\":\"AYE\",\"Other\":\"test\"}]}".getBytes(StandardCharsets.UTF_8));assertFalse(rows.isEmpty());assertTrue(new RoadWorksValidator().isValid(rows.get(0)));}}
