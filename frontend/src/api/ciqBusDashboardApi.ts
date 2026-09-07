import axios from 'axios'

export interface CiqBusRealtimeEvent {
  matchedEventId: string
  routeNo: string
  operatorCode?: string
  directionCode: 'SG_TO_JB' | 'JB_TO_SG' | string
  latitude?: number
  longitude?: number
  lastSeenTime?: string
  estimatedArrival?: string
  sourceStopCode?: string
  entryStopCode?: string
  exitStopCode?: string
  observationCount: number
  confidence?: number
  status?: string
}

export interface CiqBusRealtimeResponse {
  available: boolean
  message?: string
  generatedAt: string
  events: CiqBusRealtimeEvent[]
}

export interface CiqBusOverview {
  date: string
  sgToJb: number
  jbToSg: number
  total: number
  incompleteCount: number
  routeCounts: Record<string, number>
  activeVehicleCount?: number
  activeRouteCount?: number
  avgCrossingSeconds?: number
  medianCrossingSeconds?: number
  p90CrossingSeconds?: number
  p95CrossingSeconds?: number
  generatedAt: string
}

export interface CiqBusHourlyPoint {
  hour: number
  label: string
  sgToJb: number
  jbToSg: number
  total: number
  avgCrossingSeconds?: number
  medianCrossingSeconds?: number
  p90CrossingSeconds?: number
  p95CrossingSeconds?: number
}

export interface CiqBusPassage {
  matchedEventId: string
  routeNo: string
  operatorCode?: string
  directionCode: string
  stopCode: string
  stopName?: string
  passTime?: string
  estimatedArrival?: string
  latitude?: number
  longitude?: number
  observationCount: number
  confidence?: number
  matchMethod?: string
  createTime?: string
}

export interface CiqBusCrossing {
  matchedEventId: string
  routeNo: string
  directionCode: string
  entryStopCode?: string
  ciqEntryTime?: string
  exitStopCode?: string
  ciqExitTime?: string
  crossingSeconds?: number
  crossingMinutes?: number
  confidence?: number
  eventStatus?: string
}

export interface CiqBusPage<T> {
  page: number
  size: number
  total: number
  totalPages: number
  items: T[]
}

export interface CiqBusStatus {
  source: string
  collectionIntervalSeconds: number
  routes: string[]
  lastCollectionTime?: string
  activeVehicleCount?: number
  activeRouteCount?: number
  status: string
  realtimeAvailable: boolean
  message?: string
  generatedAt: string
}

const http = axios.create({ baseURL: '/api/ciqbus', timeout: 30000 })

export const ciqBusDashboardApi = {
  realtime: () => http.get<CiqBusRealtimeResponse>('/realtime').then(r => r.data),
  overview: (date?: string) => http.get<CiqBusOverview>('/overview', { params: { date } }).then(r => r.data),
  hourly: (date?: string) => http.get<CiqBusHourlyPoint[]>('/hourly', { params: { date } }).then(r => r.data),
  passages: (params: { date: string; route?: string; direction?: string; page: number; size: number }) =>
    http.get<CiqBusPage<CiqBusPassage>>('/passages', { params }).then(r => r.data),
  crossings: (params: { date: string; route?: string; direction?: string; page: number; size: number }) =>
    http.get<CiqBusPage<CiqBusCrossing>>('/crossings', { params }).then(r => r.data),
  status: () => http.get<CiqBusStatus>('/status').then(r => r.data)
}
