package com.mytransitgps.modules.ciq.parser;
import com.fasterxml.jackson.databind.JsonNode;import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;import java.time.*;import java.time.format.*;import java.util.*;
/** CIQ Parser 共用的纯 JSON/时间读取函数。 */
final class CiqJsonParserSupport {
 private CiqJsonParserSupport(){}
 static JsonNode root(ObjectMapper m,byte[] b){try{return m.readTree(b);}catch(Exception e){throw new IllegalArgumentException("响应不是合法JSON",e);}}
 static List<JsonNode> values(JsonNode r){JsonNode v=r!=null&&r.isObject()?r.get("value"):r;if(v==null||!v.isArray())return List.of();List<JsonNode>x=new ArrayList<>();v.forEach(x::add);return x;}
 static String text(JsonNode n,String f){JsonNode v=n.get(f);return v==null||v.isNull()?null:v.asText(null);}
 static short s(JsonNode n,String f){JsonNode v=n.get(f);return v==null||v.isNull()?0:(short)v.asInt();}
 static Short sn(JsonNode n,String f){JsonNode v=n.get(f);return v==null||v.isNull()||v.asText().isBlank()?null:(short)v.asInt();}
 static BigDecimal bd(JsonNode n,String f){String x=text(n,f);try{return x==null||x.isBlank()?null:new BigDecimal(x);}catch(Exception e){return null;}}
 static LocalDate date(JsonNode n,String f){String x=text(n,f);if(x==null||x.isBlank())return null;try{return LocalDate.parse(x.substring(0,10));}catch(Exception e){return null;}}
 static Instant instant(JsonNode n,String f){String x=text(n,f);if(x==null||x.isBlank())return null;List<DateTimeFormatter> fs=List.of(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S"),DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));for(DateTimeFormatter fmt:fs){try{return LocalDateTime.parse(x,fmt).atZone(ZoneId.of("Asia/Singapore")).toInstant();}catch(Exception ignored){}}try{return Instant.parse(x);}catch(Exception e){return null;}}
}
