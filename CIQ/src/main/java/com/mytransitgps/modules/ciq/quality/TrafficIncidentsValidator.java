package com.mytransitgps.modules.ciq.quality;
import com.mytransitgps.modules.ciq.domain.TrafficIncidentRecord;
/** 独立 Validator：关键字段、坐标或日期边界检查。 */
public class TrafficIncidentsValidator { public boolean isValid(TrafficIncidentRecord r){return r!=null&&(r.type()!=null&&!r.type().isBlank()&&r.message()!=null&&!r.message().isBlank()&&r.latitude()!=null&&r.longitude()!=null&&r.latitude().doubleValue()>=-90&&r.latitude().doubleValue()<=90&&r.longitude().doubleValue()>=-180&&r.longitude().doubleValue()<=180);} }
