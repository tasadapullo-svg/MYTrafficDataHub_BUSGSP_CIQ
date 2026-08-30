package com.mytransitgps.dashboard.provider;

import com.mytransitgps.platform.collection.ModuleCode;
import com.mytransitgps.platform.monitoring.ModuleMonitoringSnapshot;

/** Dashboard模块数据提供者扩展点，不改变现有Dashboard接口和查询实现。 */
public interface DashboardModuleProvider {
    ModuleCode moduleCode();
    ModuleMonitoringSnapshot getSnapshot();
}
