import axios from 'axios'
import type { CityCode } from '../config/cities'
import { adaptAcquisition, adaptAnomalies, adaptOverview, adaptRequests, adaptRouteTraces, adaptStatus, adaptTrend, adaptVehicles } from './adapters/busGpsAdapter'

export interface Vehicle { cityCode:string; cityName:string; vehicleId:string; vehicleLabel?:string; licensePlate?:string; tripId?:string; resolvedRouteId?:string; routeShortName?:string; routeLongName?:string; directionId?:number; latitude:number; longitude:number; speedKmh?:number; vehicleTime?:string; lastSeenTime?:string; freshnessSeconds?:number; qcFlags:string[] }
export interface VehiclesResponse { cityCode:string; generatedAt:string; count:number; vehicles:Vehicle[] }
export interface RoutePoint { latitude:number; longitude:number; vehicleTime?:string }
export interface RouteShape { routeKey:string; directionId?:number; shapeId:string; points:RoutePoint[] }
export interface RouteTrace { routeKey:string; routeShortName?:string; routeLongName?:string; vehicleId:string; directionId?:number; points:RoutePoint[] }
export interface RouteTracesResponse { cityCode:string; generatedAt:string; selectedRouteKey?:string; selectedRouteShortName?:string; selectedRouteLongName?:string; routeCount:number; segmentCount:number; historyWindowMinutes:number; roadShapes:RouteShape[]; traces:RouteTrace[] }
export interface Acquisition { cityCode:string; cityName:string; latestRequest?:string; apiStatus:string; todayRecords:number; yesterdayRecords:number; insertedRecords:number; anomalies:number; requestCount:number; successfulRequests:number }
export interface TrendPoint { cityCode:string; bucketTime:string; recordCount:number }
export interface Overview { activeVehicles:number; todayRecords:number; yesterdayRecords:number; totalRecords:number; totalRecordsEstimated:boolean; latestGpsUpdate?:string; abnormalVehicles:number; apiSuccessRate?:number; activeVehiclesDefinition:string; abnormalVehiclesDefinition:string; generatedAt:string }
export interface Anomaly { time:string; cityCode:string; cityName:string; vehicleId:string; busRoute:string; anomalyType:string; details:string; status:string }
export interface ApiRequest { time:string; cityCode:string; cityName:string; api:string; httpStatus?:number; responseTimeMs?:number; status:string }
export interface Availability<T> { available:boolean; data:T; message?:string }
export interface SystemStatus { status:'NORMAL'|'WARNING'|'ERROR'; databaseAvailable:boolean; latestGpsUpdate?:string; latestDataAgeSeconds:number; message:string; checkedAt:string }

const http = axios.create({ baseURL: '/api/dashboard', timeout: 10000 })
export const dashboardApi = {
  vehicles: (city:CityCode) => http.get('/vehicles', { params:{ city } }).then(r=>adaptVehicles(r.data)),
  routeTraces: (city:CityCode, route?:string, all=false) => http.get('/route-traces', { params:{ city, route, all } }).then(r=>adaptRouteTraces(r.data)),
  acquisition: () => http.get('/acquisition').then(r=>adaptAcquisition(r.data)),
  trend: () => http.get('/acquisition/trend').then(r=>adaptTrend(r.data)),
  overview: () => http.get('/overview').then(r=>adaptOverview(r.data)),
  anomalies: () => http.get('/anomalies/recent').then(r=>adaptAnomalies(r.data)),
  requests: () => http.get('/api-requests/recent').then(r=>adaptRequests(r.data)),
  status: () => http.get('/system-status').then(r=>adaptStatus(r.data))
}
