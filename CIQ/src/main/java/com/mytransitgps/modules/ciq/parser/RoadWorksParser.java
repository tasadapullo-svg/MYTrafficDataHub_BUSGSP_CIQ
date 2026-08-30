package com.mytransitgps.modules.ciq.parser;
import com.fasterxml.jackson.databind.ObjectMapper;import com.mytransitgps.modules.ciq.domain.RoadWorkRecord;import java.util.*;
/** 独立 Parser：只解析字段，不访问网络/数据库。 */
public class RoadWorksParser { private final ObjectMapper mapper; public RoadWorksParser(ObjectMapper m){mapper=m;} public List<RoadWorkRecord> parse(byte[] body){var root=CiqJsonParserSupport.root(mapper,body);List<RoadWorkRecord> out=new ArrayList<>();for(var n:CiqJsonParserSupport.values(root))out.add(new RoadWorkRecord(CiqJsonParserSupport.text(n,"EventID"),CiqJsonParserSupport.date(n,"StartDate"),CiqJsonParserSupport.date(n,"EndDate"),CiqJsonParserSupport.text(n,"SvcDept"),CiqJsonParserSupport.text(n,"RoadName"),CiqJsonParserSupport.text(n,"Other")));return out;} }
