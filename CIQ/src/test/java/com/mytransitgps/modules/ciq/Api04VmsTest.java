package com.mytransitgps.modules.ciq;
import static org.junit.jupiter.api.Assertions.*;import com.fasterxml.jackson.databind.ObjectMapper;import com.mytransitgps.modules.ciq.parser.VmsParser;import com.mytransitgps.modules.ciq.quality.VmsValidator;import java.nio.charset.StandardCharsets;import org.junit.jupiter.api.Test;
/** 单接口 Parser + Validator 节点测试。 */
class Api04VmsTest {@Test void parsesAndValidatesFixture(){var rows=new VmsParser(new ObjectMapper()).parse("{\"value\":[{\"EquipmentID\":\"TID_0002\",\"Latitude\":1.357022,\"Longitude\":103.902041,\"Message\":\"DRIVE WITH CARE\"}]}".getBytes(StandardCharsets.UTF_8));assertFalse(rows.isEmpty());assertTrue(new VmsValidator().isValid(rows.get(0)));}}
