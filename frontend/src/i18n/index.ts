import { createI18n } from 'vue-i18n'

const zh = {
  title: '马来西亚公交 GPS 数据采集监控大屏', ciqTitle:'马来西亚 CIQ 数据采集与接口审计大屏', busGps:'公交 GPS',ciqData:'CIQ 数据',systemNormal: '系统运行正常', systemWarning: '数据更新延迟', systemError: '系统连接异常',
  routeMonitor: '城市公交线路展示', acquisitionMonitor: '数据采集监控', operationMonitor: '运行统计与异常监控',
  fullScreen: '全屏', exitFullScreen: '退出全屏', loading: '加载中…', retry: '重试', unavailable: '数据暂不可用',
  autoRefresh: '车辆和运行线路每 5 分钟自动刷新', activeVehicles: '在线车辆数', activeRoutes: '运行线路', observedRoute: '历史 GPS 观测轨迹', lastUpdated: '最近更新', focusedRoute: '聚焦线路', routeVehicles: '展示车辆', singleRouteHint: '单线路道路轨迹，每 5 分钟刷新', allRoutesHint: '全部运行线路，每 5 分钟刷新', showAllRoutes: '展示全部路线', backToSingleRoute: '返回单线路', gtfsRoadRoute: 'GTFS 道路线路', gpsFallbackRoute: 'GPS 备用线路', latestPosition: '方向最新点位', latestDirectionVehicle: '方向最新公交车',
  latestRequest: '最新请求时间', apiStatus: 'API 状态', todayRecords: '今日记录数', yesterdayRecords: '昨日记录数', insertedRecords: '入库数', anomalies: '异常数',
  trend: '近期采集数据趋势（每 5 分钟）', totalRecords: '数据累计记录数', latestGps: '最近 GPS 更新时间', abnormalVehicles: '最近 1 小时异常车辆数', apiSuccessRate: 'API 请求成功率', estimated: '估算',
  recentAnomalies: '最近异常车辆', recentRequests: '最近 API 请求', time: '时间', city: '城市', vehicleId: '车辆 ID', route: '公交线路', type: '异常类型', details: '详情', status: '状态', api: 'API', httpStatus: '状态码', responseTime: '响应耗时',
  vehicleInfo: '车辆信息', vehicleLabel: '车辆标签', licensePlate: '车牌', routeName: '线路名称', tripId: '班次 ID', direction: '方向', latitude: '纬度', longitude: '经度', speed: '速度', vehicleTime: '车辆时间', lastSeen: '最后接收', freshness: '新鲜度', qc: '质量状态'
}
const en = {
  title: 'Malaysia Public Bus GPS Data Acquisition Monitoring Dashboard', ciqTitle:'Malaysia CIQ Data Acquisition & API Audit Dashboard',busGps:'Bus GPS',ciqData:'CIQ Data',systemNormal: 'System Normal', systemWarning: 'Data Update Delayed', systemError: 'System Connection Error',
  routeMonitor: 'City Bus Route Monitoring', acquisitionMonitor: 'Data Acquisition Monitoring', operationMonitor: 'Operational Statistics & Anomaly Monitoring',
  fullScreen: 'Full Screen', exitFullScreen: 'Exit Full Screen', loading: 'Loading…', retry: 'Retry', unavailable: 'Data unavailable',
  autoRefresh: 'Vehicles and active routes refresh every 5 minutes', activeVehicles: 'Active Vehicles', activeRoutes: 'Active Routes', observedRoute: 'Observed GPS trace', lastUpdated: 'Last Updated', focusedRoute: 'Focused Route', routeVehicles: 'Shown Vehicles', singleRouteHint: 'Single road-aligned route · 5-minute refresh', allRoutesHint: 'All active routes · 5-minute refresh', showAllRoutes: 'Show All Routes', backToSingleRoute: 'Back to Single Route', gtfsRoadRoute: 'GTFS road route', gpsFallbackRoute: 'GPS fallback route', latestPosition: 'Latest direction position', latestDirectionVehicle: 'Latest direction bus',
  latestRequest: 'Latest Request', apiStatus: 'API Status', todayRecords: "Today's Records", yesterdayRecords: "Yesterday's Records", insertedRecords: 'Inserted Records', anomalies: 'Anomalies',
  trend: 'Recent Acquisition Trend (5-minute intervals)', totalRecords: 'Total Database Records', latestGps: 'Latest GPS Update', abnormalVehicles: 'Abnormal Vehicles - Last 1 Hour', apiSuccessRate: 'API Success Rate', estimated: 'estimated',
  recentAnomalies: 'Recent Vehicle Anomalies', recentRequests: 'Recent API Requests', time: 'Time', city: 'City', vehicleId: 'Vehicle ID', route: 'Bus Route', type: 'Anomaly Type', details: 'Details', status: 'Status', api: 'API', httpStatus: 'HTTP Status', responseTime: 'Response Time',
  vehicleInfo: 'Vehicle Information', vehicleLabel: 'Vehicle Label', licensePlate: 'License Plate', routeName: 'Route Name', tripId: 'Trip ID', direction: 'Direction', latitude: 'Latitude', longitude: 'Longitude', speed: 'Speed', vehicleTime: 'Vehicle Time', lastSeen: 'Last Seen', freshness: 'Freshness', qc: 'QC Status'
}

export default createI18n({ legacy: false, locale: 'zh', fallbackLocale: 'en', messages: { zh, en } })
