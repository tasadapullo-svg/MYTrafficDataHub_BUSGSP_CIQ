package com.mytransitgps.modules.ciq.quality;
import com.mytransitgps.modules.ciq.domain.TrafficFlowLinkRecord;
/** 独立 Validator：关键字段、坐标或日期边界检查。 */
public class TrafficFlowValidator { public boolean isValid(TrafficFlowLinkRecord r){return r!=null&&(r.link()!=null&&!r.link().isBlank()&&r.link().startsWith("https://"));} }
