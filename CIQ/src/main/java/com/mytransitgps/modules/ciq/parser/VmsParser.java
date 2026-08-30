package com.mytransitgps.modules.ciq.parser;
import com.fasterxml.jackson.databind.ObjectMapper;import com.mytransitgps.modules.ciq.domain.VmsRecord;import java.util.*;
/** 独立 Parser：只解析字段，不访问网络/数据库。 */
public class VmsParser { private final ObjectMapper mapper; public VmsParser(ObjectMapper m){mapper=m;} public List<VmsRecord> parse(byte[] body){var root=CiqJsonParserSupport.root(mapper,body);List<VmsRecord> out=new ArrayList<>();for(var n:CiqJsonParserSupport.values(root))out.add(new VmsRecord(CiqJsonParserSupport.text(n,"EquipmentID"),CiqJsonParserSupport.bd(n,"Latitude"),CiqJsonParserSupport.bd(n,"Longitude"),CiqJsonParserSupport.text(n,"Message")));return out;} }
