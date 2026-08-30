package com.mytransitgps.modules.ciq.quality;
import com.mytransitgps.modules.ciq.domain.FaultyTrafficLightRecord;
/** 独立 Validator：关键字段、坐标或日期边界检查。 */
public class FaultyTrafficLightsValidator { public boolean isValid(FaultyTrafficLightRecord r){return r!=null&&(r.alarmId()!=null&&!r.alarmId().isBlank()&&r.nodeId()!=null&&!r.nodeId().isBlank()&&r.startDate()!=null);} }
