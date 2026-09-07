<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import DashboardHeader from '../components/dashboard/DashboardHeader.vue'
import CiqBusMap from '../components/ciq/CiqBusMap.vue'
import CiqBusHourlyChart from '../components/ciq/CiqBusHourlyChart.vue'
import {
  ciqBusDashboardApi,
  type CiqBusCrossing,
  type CiqBusHourlyPoint,
  type CiqBusOverview,
  type CiqBusPage,
  type CiqBusPassage,
  type CiqBusRealtimeEvent,
  type CiqBusStatus
} from '../api/ciqBusDashboardApi'

const { t, locale } = useI18n()
const routes = ['160', '170', '170X', '950']
const today = () => new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Kuala_Lumpur', year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date())

const realtime = ref<CiqBusRealtimeEvent[]>([])
const overview = ref<CiqBusOverview>()
const status = ref<CiqBusStatus>()
const hourly = ref<CiqBusHourlyPoint[]>([])
const realtimeLoading = ref(true)
const overviewLoading = ref(true)
const hourlyLoading = ref(true)
const realtimeError = ref(false)
const overviewError = ref(false)
const hourlyError = ref(false)
const date = ref(today())
const historyDate = ref(today())
const historyRoute = ref('ALL')
const historyDirection = ref('ALL')
const historyType = ref<'passages' | 'crossings'>('passages')
const historyPage = ref(0)
const historySize = 20
const passagePage = ref<CiqBusPage<CiqBusPassage>>()
const crossingPage = ref<CiqBusPage<CiqBusCrossing>>()
const historyLoading = ref(false)
const historyError = ref(false)

function number(value?: number) {
  return value == null ? '--' : new Intl.NumberFormat(locale.value === 'zh' ? 'zh-CN' : 'en-US', { maximumFractionDigits: 2 }).format(value)
}
function minutes(value?: number) { return value == null ? '--' : `${(value / 60).toFixed(1)} ${t('ciqBusPage.minutes')}` }
function time(value?: string) {
  return value ? new Intl.DateTimeFormat(locale.value === 'zh' ? 'zh-CN' : 'en-GB', {
    timeZone: 'Asia/Kuala_Lumpur', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false
  }).format(new Date(value)) : '--'
}
function field(value: unknown) { return value == null || value === '' ? '--' : String(value) }
function statusClass(value?: string) { return String(value || '').toLowerCase() }

async function loadRealtime() {
  realtimeLoading.value = true
  try {
    const data = await ciqBusDashboardApi.realtime()
    realtime.value = data.events || []
    realtimeError.value = !data.available
  } catch {
    realtimeError.value = true
  } finally { realtimeLoading.value = false }
}

async function loadOverview() {
  overviewLoading.value = true
  try { overview.value = await ciqBusDashboardApi.overview(date.value); overviewError.value = false }
  catch { overviewError.value = true }
  finally { overviewLoading.value = false }
}

async function loadStatus() {
  try { status.value = await ciqBusDashboardApi.status() }
  catch { status.value = undefined }
}

async function loadHourly() {
  hourlyLoading.value = true
  try { hourly.value = await ciqBusDashboardApi.hourly(date.value); hourlyError.value = false }
  catch { hourlyError.value = true }
  finally { hourlyLoading.value = false }
}

async function changeDate() { await Promise.all([loadOverview(), loadHourly()]) }

async function loadHistory(reset = false) {
  if (reset) historyPage.value = 0
  historyLoading.value = true
  historyError.value = false
  const params = {
    date: historyDate.value,
    route: historyRoute.value,
    direction: historyDirection.value,
    page: historyPage.value,
    size: historySize
  }
  try {
    if (historyType.value === 'passages') passagePage.value = await ciqBusDashboardApi.passages(params)
    else crossingPage.value = await ciqBusDashboardApi.crossings(params)
  } catch { historyError.value = true }
  finally { historyLoading.value = false }
}

async function changePage(delta: number) {
  const target = historyPage.value + delta
  const totalPages = historyType.value === 'passages' ? passagePage.value?.totalPages : crossingPage.value?.totalPages
  if (target < 0 || (totalPages != null && target >= totalPages)) return
  historyPage.value = target
  await loadHistory(false)
}

const activePage = computed(() => historyType.value === 'passages' ? passagePage.value : crossingPage.value)
const systemStatus = computed(() => ({
  status: (!status.value || status.value.status === 'REALTIME_UNAVAILABLE') ? 'ERROR' as const : status.value.status === 'NORMAL' ? 'NORMAL' as const : 'WARNING' as const,
  databaseAvailable: !overviewError.value,
  latestDataAgeSeconds: 0,
  message: status.value?.message || '',
  checkedAt: new Date().toISOString()
}))

let realtimeTimer: number
let overviewTimer: number
onMounted(async () => {
  await Promise.all([loadRealtime(), loadOverview(), loadStatus(), loadHourly()])
  await loadHistory(true)
  realtimeTimer = window.setInterval(() => { loadRealtime(); loadStatus() }, 45000)
  overviewTimer = window.setInterval(() => { loadOverview(); loadHourly() }, 60000)
})
onBeforeUnmount(() => { window.clearInterval(realtimeTimer); window.clearInterval(overviewTimer) })
</script>

<template>
  <main class="dashboard-page ciqbus-page">
    <DashboardHeader mode="ciqbus" :status="systemStatus" />
    <p class="ciqbus-subtitle">{{ t('ciqBusPage.subtitle') }}</p>

    <section class="ciqbus-summary-grid">
      <article class="tech-panel ciqbus-overview-panel">
        <h2><b>1</b>{{ t('ciqBusPage.todayOverview') }}</h2>
        <div v-if="overviewLoading" class="panel-state">{{ t('loading') }}</div>
        <div v-else-if="overviewError" class="panel-state error">{{ t('unavailable') }}</div>
        <div class="ciqbus-kpis">
          <div class="ciqbus-kpi accent"><label>{{ t('ciqBusPage.sgToJbToday') }}</label><strong>{{ number(overview?.sgToJb) }}</strong></div>
          <div class="ciqbus-kpi accent"><label>{{ t('ciqBusPage.jbToSgToday') }}</label><strong>{{ number(overview?.jbToSg) }}</strong></div>
          <div class="ciqbus-kpi success"><label>{{ t('ciqBusPage.totalToday') }}</label><strong>{{ number(overview?.total) }}</strong></div>
          <div class="ciqbus-kpi"><label>{{ t('ciqBusPage.activeVehicleCount') }}</label><strong>{{ number(overview?.activeVehicleCount) }}</strong></div>
          <div class="ciqbus-kpi"><label>{{ t('ciqBusPage.activeRouteCount') }}</label><strong>{{ number(overview?.activeRouteCount) }}</strong></div>
          <div class="ciqbus-kpi"><label>{{ t('ciqBusPage.avgCrossingTime') }}</label><strong>{{ minutes(overview?.avgCrossingSeconds) }}</strong></div>
          <div class="ciqbus-kpi"><label>{{ t('ciqBusPage.median') }}</label><strong>{{ minutes(overview?.medianCrossingSeconds) }}</strong></div>
          <div class="ciqbus-kpi"><label>{{ t('ciqBusPage.p90') }}</label><strong>{{ minutes(overview?.p90CrossingSeconds) }}</strong></div>
          <div class="ciqbus-kpi"><label>{{ t('ciqBusPage.p95') }}</label><strong>{{ minutes(overview?.p95CrossingSeconds) }}</strong></div>
          <div class="ciqbus-kpi warning"><label>{{ t('ciqBusPage.incompleteCount') }}</label><strong>{{ number(overview?.incompleteCount) }}</strong></div>
        </div>
        <div class="ciqbus-route-counts">
          <div v-for="route in routes" :key="route"><span>{{ route }}</span><strong>{{ number(overview?.routeCounts?.[route]) }}</strong></div>
        </div>
      </article>

      <article class="tech-panel ciqbus-status-panel">
        <h2><b>2</b>{{ t('ciqBusPage.realtimeStatus') }}</h2>
        <div class="ciqbus-status-body">
          <div class="ciqbus-status-grid">
            <div><span>{{ t('ciqBusPage.source') }}</span><b>{{ status?.source ?? 'LTA BusArrival' }}</b></div>
            <div><span>{{ t('ciqBusPage.collectionFrequency') }}</span><b>{{ t('ciqBusPage.twoMinutes') }}</b></div>
            <div><span>{{ t('ciqBusPage.realtimeRoutes') }}</span><b>160 / 170 / 170X / 950</b></div>
            <div><span>{{ t('ciqBusPage.lastCollectionTime') }}</span><b>{{ time(status?.lastCollectionTime) }}</b></div>
            <div><span>{{ t('ciqBusPage.activeVehicleCount') }}</span><b>{{ number(status?.activeVehicleCount) }}</b></div>
            <div><span>{{ t('ciqBusPage.activeRouteCount') }}</span><b>{{ number(status?.activeRouteCount) }}</b></div>
            <div><span>{{ t('ciqBusPage.collectionStatus') }}</span><b><span class="ciqbus-status-pill" :class="statusClass(status?.status)">{{ status?.status ?? '--' }}</span></b></div>
          </div>
          <p class="ciqbus-note">{{ t('ciqBusPage.dataNote') }}</p>
        </div>
      </article>
    </section>

    <section class="ciqbus-live-grid">
      <article class="tech-panel ciqbus-map-panel">
        <h2><b>3</b>{{ t('ciqBusPage.realtimeMap') }}</h2>
        <CiqBusMap :events="realtime" :loading="realtimeLoading" :error="realtimeError ? t('ciqBusPage.realtimeUnavailable') : undefined" @retry="loadRealtime" />
      </article>

      <article class="tech-panel ciqbus-trend-panel">
        <h2><b>4</b>{{ t('ciqBusPage.hourlyTrend') }}</h2>
        <div class="ciqbus-hourly-tools"><label>{{ t('ciqBusPage.date') }}</label><input v-model="date" type="date" @change="changeDate"></div>
        <div v-if="hourlyLoading" class="panel-state">{{ t('loading') }}</div>
        <div v-else-if="hourlyError" class="panel-state error">{{ t('unavailable') }}</div>
        <CiqBusHourlyChart :points="hourly" />
      </article>
    </section>

    <section class="tech-panel ciqbus-history">
      <h2><b>5</b>{{ t('ciqBusPage.historyQuery') }}</h2>
      <div class="ciqbus-history-filter">
        <input v-model="historyDate" type="date">
        <select v-model="historyRoute"><option value="ALL">ALL</option><option v-for="route in routes" :key="route" :value="route">{{ route }}</option></select>
        <select v-model="historyDirection"><option value="ALL">ALL</option><option value="SG_TO_JB">SG_TO_JB</option><option value="JB_TO_SG">JB_TO_SG</option></select>
        <select v-model="historyType"><option value="passages">{{ t('ciqBusPage.passageRecords') }}</option><option value="crossings">{{ t('ciqBusPage.crossingRecords') }}</option></select>
        <button @click="loadHistory(true)">{{ t('ciqBusPage.search') }}</button>
      </div>

      <div class="ciqbus-history-table">
        <div v-if="historyLoading" class="ciqbus-history-state">{{ t('ciqBusPage.loadingHistory') }}</div>
        <div v-else-if="historyError" class="ciqbus-history-state error">{{ t('ciqBusPage.historyUnavailable') }}</div>
        <div v-else-if="!activePage?.items?.length" class="ciqbus-history-state">{{ t('ciqBusPage.noData') }}</div>
        <table v-else-if="historyType === 'passages'">
          <thead><tr><th>{{ t('ciqBusPage.matchedEventId') }}</th><th>{{ t('ciqBusPage.routeNo') }}</th><th>{{ t('ciqBusPage.operatorCode') }}</th><th>{{ t('ciqBusPage.directionCode') }}</th><th>{{ t('ciqBusPage.stopCode') }}</th><th>{{ t('ciqBusPage.stopName') }}</th><th>{{ t('ciqBusPage.passTime') }}</th><th>{{ t('ciqBusPage.estimatedArrival') }}</th><th>{{ t('latitude') }}</th><th>{{ t('longitude') }}</th><th>{{ t('ciqBusPage.observationCount') }}</th><th>{{ t('ciqBusPage.confidence') }}</th><th>{{ t('ciqBusPage.matchMethod') }}</th><th>{{ t('ciqBusPage.createTime') }}</th></tr></thead>
          <tbody><tr v-for="row in passagePage?.items" :key="row.matchedEventId + row.stopCode + row.passTime"><td class="event-id-cell" :title="row.matchedEventId">{{ field(row.matchedEventId) }}</td><td>{{ field(row.routeNo) }}</td><td>{{ field(row.operatorCode) }}</td><td>{{ field(row.directionCode) }}</td><td>{{ field(row.stopCode) }}</td><td>{{ field(row.stopName) }}</td><td>{{ time(row.passTime) }}</td><td>{{ time(row.estimatedArrival) }}</td><td>{{ row.latitude == null ? '--' : row.latitude.toFixed(6) }}</td><td>{{ row.longitude == null ? '--' : row.longitude.toFixed(6) }}</td><td>{{ number(row.observationCount) }}</td><td>{{ row.confidence == null ? '--' : row.confidence.toFixed(3) }}</td><td>{{ field(row.matchMethod) }}</td><td>{{ time(row.createTime) }}</td></tr></tbody>
        </table>
        <table v-else>
          <thead><tr><th>{{ t('ciqBusPage.matchedEventId') }}</th><th>{{ t('ciqBusPage.routeNo') }}</th><th>{{ t('ciqBusPage.directionCode') }}</th><th>{{ t('ciqBusPage.entryStopCode') }}</th><th>{{ t('ciqBusPage.ciqEntryTime') }}</th><th>{{ t('ciqBusPage.exitStopCode') }}</th><th>{{ t('ciqBusPage.ciqExitTime') }}</th><th>{{ t('ciqBusPage.crossingSeconds') }}</th><th>{{ t('ciqBusPage.crossingMinutes') }}</th><th>{{ t('ciqBusPage.confidence') }}</th><th>{{ t('ciqBusPage.eventStatus') }}</th></tr></thead>
          <tbody><tr v-for="row in crossingPage?.items" :key="row.matchedEventId + row.ciqEntryTime"><td class="event-id-cell" :title="row.matchedEventId">{{ field(row.matchedEventId) }}</td><td>{{ field(row.routeNo) }}</td><td>{{ field(row.directionCode) }}</td><td>{{ field(row.entryStopCode) }}</td><td>{{ time(row.ciqEntryTime) }}</td><td>{{ field(row.exitStopCode) }}</td><td>{{ time(row.ciqExitTime) }}</td><td>{{ field(row.crossingSeconds) }}</td><td>{{ row.crossingMinutes == null ? '--' : row.crossingMinutes.toFixed(2) }}</td><td>{{ row.confidence == null ? '--' : row.confidence.toFixed(3) }}</td><td><span class="ciqbus-status-pill" :class="statusClass(row.eventStatus)">{{ field(row.eventStatus) }}</span></td></tr></tbody>
        </table>
      </div>
      <div class="ciqbus-pagination"><span>{{ t('ciqBusPage.totalRows') }}: {{ activePage?.total ?? 0 }}</span><button :disabled="historyPage <= 0" @click="changePage(-1)">{{ t('ciqBusPage.previous') }}</button><span>{{ t('ciqBusPage.page') }} {{ historyPage + 1 }} / {{ Math.max(1, activePage?.totalPages ?? 1) }}</span><button :disabled="historyPage + 1 >= (activePage?.totalPages ?? 0)" @click="changePage(1)">{{ t('ciqBusPage.next') }}</button></div>
    </section>
  </main>
</template>
