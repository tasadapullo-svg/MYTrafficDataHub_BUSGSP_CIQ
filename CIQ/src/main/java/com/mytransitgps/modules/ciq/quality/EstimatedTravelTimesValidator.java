package com.mytransitgps.modules.ciq.quality;
import com.mytransitgps.modules.ciq.domain.EstimatedTravelTimeRecord;
/** 独立 Validator：关键字段、坐标或日期边界检查。 */
public class EstimatedTravelTimesValidator { public boolean isValid(EstimatedTravelTimeRecord r){return r!=null&&(r.name()!=null&&!r.name().isBlank()&&r.farEndPoint()!=null&&r.startPoint()!=null&&r.endPoint()!=null&&r.direction()>0&&r.estTime()>=0);} }
