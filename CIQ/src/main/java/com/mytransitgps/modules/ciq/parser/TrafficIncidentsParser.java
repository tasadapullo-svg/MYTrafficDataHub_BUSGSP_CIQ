package com.mytransitgps.modules.ciq.parser;
import com.fasterxml.jackson.databind.ObjectMapper;import com.mytransitgps.modules.ciq.domain.TrafficIncidentRecord;import java.util.*;
/** 独立 Parser：只解析字段，不访问网络/数据库。 */
public class TrafficIncidentsParser { private final ObjectMapper mapper; public TrafficIncidentsParser(ObjectMapper m){mapper=m;} public List<TrafficIncidentRecord> parse(byte[] body){var root=CiqJsonParserSupport.root(mapper,body);List<TrafficIncidentRecord> out=new ArrayList<>();for(var n:CiqJsonParserSupport.values(root))out.add(new TrafficIncidentRecord(CiqJsonParserSupport.text(n,"Type"),CiqJsonParserSupport.bd(n,"Latitude"),CiqJsonParserSupport.bd(n,"Longitude"),CiqJsonParserSupport.text(n,"Message")));return out;} }
