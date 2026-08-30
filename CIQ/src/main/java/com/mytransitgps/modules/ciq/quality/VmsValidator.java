package com.mytransitgps.modules.ciq.quality;
import com.mytransitgps.modules.ciq.domain.VmsRecord;
/** 独立 Validator：关键字段、坐标或日期边界检查。 */
public class VmsValidator { public boolean isValid(VmsRecord r){return r!=null&&(r.equipmentId()!=null&&!r.equipmentId().isBlank()&&r.latitude()!=null&&r.longitude()!=null&&r.latitude().doubleValue()>=-90&&r.latitude().doubleValue()<=90&&r.longitude().doubleValue()>=-180&&r.longitude().doubleValue()<=180);} }
