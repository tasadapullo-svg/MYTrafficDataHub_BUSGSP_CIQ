package com.mytransitgps.modules.ciq.parser;
import com.fasterxml.jackson.databind.ObjectMapper;import com.mytransitgps.modules.ciq.domain.TrafficFlowLinkRecord;import java.util.*;
/** 独立 Parser：只解析字段，不访问网络/数据库。 */
public class TrafficFlowParser { private final ObjectMapper mapper; public TrafficFlowParser(ObjectMapper m){mapper=m;} public List<TrafficFlowLinkRecord> parse(byte[] body){var root=CiqJsonParserSupport.root(mapper,body);List<TrafficFlowLinkRecord> out=new ArrayList<>();for(var n:CiqJsonParserSupport.values(root))out.add(new TrafficFlowLinkRecord(CiqJsonParserSupport.text(n,"Link")));return out;} }
