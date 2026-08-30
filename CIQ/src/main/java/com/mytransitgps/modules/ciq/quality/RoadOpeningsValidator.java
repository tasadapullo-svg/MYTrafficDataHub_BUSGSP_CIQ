package com.mytransitgps.modules.ciq.quality;
import com.mytransitgps.modules.ciq.domain.RoadOpeningRecord;
/** 独立 Validator：关键字段、坐标或日期边界检查。 */
public class RoadOpeningsValidator { public boolean isValid(RoadOpeningRecord r){return r!=null&&(r.eventId()!=null&&!r.eventId().isBlank()&&r.startDate()!=null&&(r.endDate()==null||!r.endDate().isBefore(r.startDate())));} }
