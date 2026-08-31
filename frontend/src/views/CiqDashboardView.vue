<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import DashboardHeader from '../components/dashboard/DashboardHeader.vue'
import CiqTrend from '../components/ciq/CiqTrend.vue'
import DiskGauge from '../components/ciq/DiskGauge.vue'
import CiqDataMap from '../components/ciq/CiqDataMap.vue'
import {
  ciqDashboardApi,
  type CiqApiStatus,
  type CiqDataRecord,
  type CiqMapPoint,
  type CiqOverview,
  type CiqRequestLog,
  type CiqStorage,
  type CiqTrendPoint
} from '../api/ciqDashboardApi'

const { locale } = useI18n()
const overview = ref<CiqOverview>()
const apis = ref<CiqApiStatus[]>([])
const trend = ref<CiqTrendPoint[]>([])
const requests = ref<CiqRequestLog[]>([])
const storage = ref<CiqStorage>()
const selected = ref<CiqApiStatus>()
const errors = ref(new Set<string>())
const hours = ref(3)

const mapPoints = ref<CiqMapPoint[]>([])
const dataRecords = ref<CiqDataRecord[]>([])
const mapApi = ref('ALL')
const dataApi = ref('ALL')
const dataRange = ref('720')
const dataCategory = ref('')
const mapLoading = ref(false)
const dataLoading = ref(false)

const n = (v?: number) => v == null
  ? '--'
  : new Intl.NumberFormat(locale.value === 'zh' ? 'zh-CN' : 'en-US', { maximumFractionDigits: 2 }).format(v)

const time = (v?: string) => v
  ? new Intl.DateTimeFormat(locale.value === 'zh' ? 'zh-CN' : 'en-GB', {
      timeZone: 'Asia/Kuala_Lumpur',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false
    }).format(new Date(v))
  : '--'

const bytes = (v?: number) => v == null
  ? '--'
  : v < 1024
    ? `${v} B`
    : v < 1048576
      ? `${(v / 1024).toFixed(1)} KB`
      : v < 1073741824
        ? `${(v / 1048576).toFixed(1)} MB`
        : `${(v / 1073741824).toFixed(2)} GB`

const currentLanguage = computed<'en' | 'zh'>(() =>
  String(locale.value).toLowerCase().startsWith('zh') ? 'zh' : 'en'
)
const label = (en: string, zh: string) => currentLanguage.value === 'zh' ? zh : en
const cls = (v?: string) => String(v || 'na').toLowerCase()
const apiCodes = ['API01', 'API02', 'API03', 'API04', 'API05', 'API06', 'API07', 'API08']

const apiDescriptions: Record<string, [string, string]> = {
  API01: ['Road Segment Real-Time Speed Bands', '道路分段实时速度等级'],
  API02: ['Expressway Segment Estimated Travel Times', '高速公路分段预计旅行时间'],
  API03: ['Real-Time Traffic Incidents', '实时交通事件'],
  API04: ['Real-Time VMS / EMAS Messages', '电子信息板实时文本'],
  API05: ['Faulty Traffic Signal Alerts', '故障交通信号灯'],
  API06: ['Approved Road Works', '已批准道路施工'],
  API07: ['Historical Hourly Average Traffic Flow', '历史小时平均交通流量'],
  API08: ['Planned Road Openings', '计划道路开放']
}

const metricNames: Record<string, [string, string]> = {
  woodlandsRecords: ['Woodlands Records', 'Woodlands 记录'],
  tuasRecords: ['Tuas Records', 'Tuas 记录'],
  activeEvents: ['Active Events', '活动事件'],
  newToday: ['New Today', '今日新增'],
  resolvedToday: ['Resolved Today', '今日解决'],
  ciqRelated: ['CIQ Related', 'CIQ 相关'],
  devices: ['Devices', '设备数'],
  activeMessages: ['Active Messages', '活动消息'],
  messageChangesToday: ['Changes Today', '今日变化'],
  activeFaults: ['Active Faults', '活动故障'],
  activeRoadWorks: ['Active Road Works', '活动施工'],
  futureRoadWorks: ['Future Road Works', '未来施工']
}

const apiDescription = (api: CiqApiStatus) =>
  apiDescriptions[api.apiCode]?.[currentLanguage.value === 'zh' ? 1 : 0]
  ?? (currentLanguage.value === 'zh' ? api.nameZh : api.apiName)

const metricName = (key: string) =>
  metricNames[key]?.[currentLanguage.value === 'zh' ? 1 : 0] ?? key

async function moduleLoad<T>(key: string, job: Promise<T>, set: (v: T) => void) {
  try {
    set(await job)
    errors.value.delete(key)
  } catch {
    errors.value.add(key)
  }
}

async function loadMap(api = mapApi.value) {
  mapApi.value = api
  mapLoading.value = true
  try {
    await moduleLoad('map', ciqDashboardApi.mapPoints(api, 1200), v => mapPoints.value = v)
  } finally {
    mapLoading.value = false
  }
}

async function loadData() {
  dataLoading.value = true
  const range = dataRange.value === 'ALL'
    ? undefined
    : new Date(Date.now() - Number(dataRange.value) * 3600000).toISOString()
  try {
    await moduleLoad(
      'data',
      ciqDashboardApi.dataRecords({
        api: dataApi.value,
        from: range,
        category: dataCategory.value || undefined,
        limit: 60
      }),
      v => dataRecords.value = v
    )
  } finally {
    dataLoading.value = false
  }
}

async function refresh() {
  await Promise.all([
    moduleLoad('overview', ciqDashboardApi.overview(), v => overview.value = v),
    moduleLoad('apis', ciqDashboardApi.apis(), v => apis.value = v),
    moduleLoad('trend', ciqDashboardApi.trend(hours.value), v => trend.value = v),
    moduleLoad('requests', ciqDashboardApi.requests(), v => requests.value = v),
    moduleLoad('storage', ciqDashboardApi.storage(), v => storage.value = v),
    loadMap(),
    loadData()
  ])
}

let timer: number
onMounted(() => {
  refresh()
  timer = window.setInterval(refresh, 60000)
})
onBeforeUnmount(() => window.clearInterval(timer))

async function setHours(v: number) {
  hours.value = v
  await moduleLoad('trend', ciqDashboardApi.trend(v), x => trend.value = x)
}

const systemStatus = computed(() => ({
  status: (errors.value.size ? 'WARNING' : 'NORMAL') as 'WARNING' | 'NORMAL',
  databaseAvailable: !errors.value.has('apis'),
  latestDataAgeSeconds: 0,
  message: '',
  checkedAt: new Date().toISOString()
}))

const latestByApi = computed(() => ['API03', 'API04', 'API05', 'API06', 'API07', 'API08']
  .map(code => ({ code, record: dataRecords.value.find(r => r.apiCode === code) })))
</script>

<template>
  <main class="dashboard-page ciq-page">
    <DashboardHeader mode="ciq" :status="systemStatus" />
    <p class="ciq-subtitle">
      {{ label('Singapore LTA DataMall CIQ-related Traffic Data Monitoring', '新加坡 LTA DataMall CIQ 相关交通数据采集监控') }}
    </p>

    <section class="ciq-top">
      <article class="tech-panel api-overview">
        <h2><b>1</b>{{ label('CIQ API Overview', 'CIQ 接口总览') }}</h2>
        <div v-if="errors.has('apis')" class="panel-state error">
          {{ label('Data unavailable', '数据暂不可用') }}
        </div>

        <div class="api-card-grid">
          <button
            v-for="api in apis"
            :key="api.apiCode"
            class="api-card"
            :class="cls(api.status)"
            @click="selected = api"
          >
            <header>
              <strong>{{ api.apiCode }} · {{ api.apiName }}</strong>
              <span>{{ api.status }}</span>
            </header>

            <small>{{ apiDescription(api) }}</small>

            <div class="api-meta">
              <span>{{ label('Cycle', '周期') }}</span><b>{{ api.schedule }}</b>
              <span>{{ label('Last Request', '最近请求') }}</span><b>{{ time(api.lastRequest) }}</b>
            </div>

            <div v-if="api.apiCode === 'API01'" class="layer-stack">
              <div>
                <label>{{ label('Raw Data Layer', '原始数据层') }}</label>
                <strong>{{ n(Number(api.metrics.lastCompleteRawRecords ?? api.rawRecords)) }}</strong>
                <small>{{ label('Last complete Singapore snapshot', '最近完整的新加坡全量快照') }}</small>
              </div>
              <div>
                <label>{{ label('Research Database Layer', '研究数据库层') }}</label>
                <strong>{{ n(Number(api.metrics.lastCompleteSelected ?? api.ciqSelected)) }}</strong>
                <small>{{ label('Woodlands + Tuas selected', 'Woodlands + Tuas 研究区筛选') }}</small>
              </div>
              <div>
                <label>{{ label('DB Inserted', '数据库入库') }}</label>
                <strong>{{ n(Number(api.metrics.lastCompleteInserted ?? api.dbInserted)) }}</strong>
                <small>{{ label('PostgreSQL rows', 'PostgreSQL 记录') }}</small>
              </div>
            </div>

            <div v-else-if="api.apiCode === 'API07'" class="file-model">
              <span>{{ label('Current Month', '当前月份') }}<b>{{ api.metrics.currentMonth ?? '--' }}</b></span>
              <span>{{ label('Last Check', '最近检查') }}<b>{{ time(api.lastRequest) }}</b></span>
              <span>{{ label('Source File', '源文件') }}<b>{{ api.rawFile ?? '--' }}</b></span>
              <span>{{ label('File Size', '文件大小') }}<b>{{ bytes(api.fileSizeBytes) }}</b></span>
              <span>SHA-256<b>{{ api.sha256 ? 'VERIFIED' : '--' }}</b></span>
              <span>{{ label('Download', '下载') }}<b>{{ api.metrics.downloadStatus ?? 'WAITING' }}</b></span>
              <span>{{ label('Parse', '解析') }}<b>{{ api.metrics.parseStatus ?? '--' }}</b></span>
              <span>{{ label('Database', '数据库') }}<b>{{ api.metrics.databaseStatus ?? '--' }}</b></span>
            </div>

            <div v-else-if="Object.keys(api.metrics).length" class="metric-grid">
              <span v-for="(value, key) in api.metrics" :key="key">
                <label>{{ metricName(String(key)) }}</label>
                <b>{{ value ?? '--' }}</b>
              </span>
            </div>

            <div v-else class="metric-grid">
              <span><label>{{ label('Raw Records', '原始记录') }}</label><b>{{ n(api.rawRecords) }}</b></span>
              <span><label>{{ label('DB Inserted', '数据库入库') }}</label><b>{{ n(api.dbInserted) }}</b></span>
              <span><label>{{ label('Executions Today', '今日执行') }}</label><b>{{ n(api.todayExecutions) }}</b></span>
              <span><label>{{ label('Completeness', '完整性') }}</label><b>{{ api.completeness }}</b></span>
            </div>
          </button>
        </div>
      </article>

      <article class="tech-panel task-panel">
        <h2><b>2</b>{{ label('Download Task Monitoring', '下载任务监控') }}</h2>
        <div class="task-kpis">
          <div><label>{{ label('Today Planned Tasks', '今日计划任务') }}</label><strong>{{ n(overview?.plannedTasks) }}</strong></div>
          <div class="success"><label>{{ label('Successful Tasks', '成功任务') }}</label><strong>{{ n(overview?.successfulTasks) }}</strong></div>
          <div class="error"><label>{{ label('Failed Tasks', '失败任务') }}</label><strong>{{ n(overview?.failedTasks) }}</strong></div>
          <div><label>{{ label('API Success Rate', 'API 成功率') }}</label><strong>{{ overview?.successRate == null ? '--' : overview.successRate.toFixed(2) + '%' }}</strong></div>
        </div>

        <div class="trend-head">
          <h3>{{ label('Recent Download Trend · 30-minute interval', '最近下载趋势 · 30分钟间隔') }}</h3>
          <select :value="hours" @change="setHours(Number(($event.target as HTMLSelectElement).value))">
            <option :value="3">{{ label('3 Hours', '3 小时') }}</option>
            <option :value="24">{{ label('Today', '今天') }}</option>
            <option :value="168">{{ label('7 Days', '7 天') }}</option>
            <option :value="720">{{ label('30 Days', '30 天') }}</option>
          </select>
        </div>
        <CiqTrend :points="trend" :hours="hours" />
      </article>
    </section>

    <section class="tech-panel ciq-explorer">
      <h2><b>3</b>{{ label('CIQ Spatial Data & Latest Records', 'CIQ 空间数据与最新记录') }}</h2>
      <div class="explorer-grid">
        <div class="spatial-panel">
          <CiqDataMap
            :points="mapPoints"
            :api="mapApi"
            :loading="mapLoading"
            :error="errors.has('map') ? label('Map data unavailable', '地图数据暂不可用') : undefined"
            :language="currentLanguage"
            @api-change="loadMap"
            @retry="loadMap()"
          />
        </div>

        <div class="latest-data-panel">
          <div class="data-filter">
            <select v-model="dataApi">
              <option value="ALL">API03–API08</option>
              <option v-for="code in apiCodes.slice(2)" :key="code" :value="code">{{ code }}</option>
            </select>
            <select v-model="dataRange">
              <option value="24">{{ label('24 Hours', '24 小时') }}</option>
              <option value="168">{{ label('7 Days', '7 天') }}</option>
              <option value="720">{{ label('30 Days', '30 天') }}</option>
              <option value="ALL">{{ label('All Time', '全部时间') }}</option>
            </select>
            <input
              v-model.trim="dataCategory"
              :placeholder="label('Category / Keyword', '分类 / 关键词')"
              @keyup.enter="loadData"
            >
            <button @click="loadData">{{ label('Search', '查询') }}</button>
          </div>

          <div class="latest-strip">
            <div v-for="item in latestByApi" :key="item.code" :class="{ empty: !item.record }">
              <strong>{{ item.code }}</strong>
              <span>{{ item.record?.category ?? label('No data', '暂无数据') }}</span>
              <small>{{ time(item.record?.eventTime) }}</small>
            </div>
          </div>

          <div v-if="dataLoading" class="data-query-state">{{ label('Loading records…', '数据查询中…') }}</div>
          <div v-else-if="errors.has('data')" class="data-query-state error">{{ label('Query failed. Please retry.', '查询失败，请重试') }}</div>
          <div v-else-if="!dataRecords.length" class="data-query-state">{{ label('No real records match the current filters.', '当前条件没有真实记录') }}</div>
          <div v-else class="data-record-list">
            <article v-for="r in dataRecords" :key="r.apiCode + r.recordId">
              <header>
                <b>{{ r.apiCode }}</b>
                <span>{{ r.category }}</span>
                <time>{{ time(r.eventTime) }}</time>
              </header>
              <h3>{{ r.title }}</h3>
              <p>{{ r.description }}</p>
              <footer>
                <span class="status-pill" :class="cls(r.status)">{{ r.status }}</span>
                <span v-for="(value, key) in r.details" :key="key">{{ key }}: {{ value ?? '--' }}</span>
              </footer>
            </article>
          </div>
        </div>
      </div>
    </section>

    <section class="ciq-bottom">
      <article class="tech-panel logs-panel">
        <h2><b>4</b>{{ label('Recent Download Logs', '最近下载日志') }}</h2>
        <div class="table-scroll">
          <table>
            <thead>
              <tr>
                <th>{{ label('Time', '时间') }}</th>
                <th>API</th>
                <th>HTTP</th>
                <th>{{ label('Raw', '原始') }}</th>
                <th>CIQ</th>
                <th>{{ label('Inserted', '入库') }}</th>
                <th>{{ label('Duration', '耗时') }}</th>
                <th>{{ label('Retry', '重试') }}</th>
                <th>{{ label('Archive', '归档') }}</th>
                <th>{{ label('Status', '状态') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in requests" :key="r.apiCode + r.time">
                <td>{{ time(r.time) }}</td>
                <td>{{ r.apiCode }} {{ r.apiName }}</td>
                <td>{{ r.httpStatus ?? '--' }}</td>
                <td>{{ n(r.rawRecords) }}</td>
                <td>{{ n(r.ciqSelected) }}</td>
                <td>{{ n(r.inserted) }}</td>
                <td>{{ r.durationMs == null ? '--' : r.durationMs + ' ms' }}</td>
                <td>{{ r.retry ?? '--' }}</td>
                <td>{{ r.archive }}</td>
                <td><span class="status-pill" :class="cls(r.status)">{{ r.status }}</span></td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>

      <article class="tech-panel storage-panel">
        <h2><b>5</b>{{ label('Archive & Storage Status', '归档与存储状态') }}</h2>
        <div class="storage-grid">
          <div>
            <label>{{ label('Raw Files', '原始文件') }}</label>
            <strong>{{ n(storage?.rawTotalFiles) }}</strong>
            <small>{{ label('Today', '今日') }} {{ n(storage?.rawTodayFiles) }} · {{ bytes(storage?.rawTotalBytes) }}</small>
          </div>
          <div>
            <label>{{ label('ZIP Archives', 'ZIP 归档') }}</label>
            <strong>{{ n(storage?.archiveCount) }}</strong>
            <small>{{ storage?.archiveStatus ?? '--' }}</small>
          </div>
          <div>
            <label>PostgreSQL</label>
            <strong>{{ n(storage?.dbTotalRows) }}</strong>
            <small>{{ label('Inserted today', '今日入库') }} · {{ n(storage?.dbTodayInserted) }}</small>
          </div>
          <div class="disk-box">
            <DiskGauge :used="storage?.diskUsagePercent" />
            <span>
              {{ label('Total', '总量') }} {{ bytes(storage?.diskTotalBytes) }}<br>
              {{ label('Used', '已用') }} {{ bytes(storage?.diskUsedBytes) }}<br>
              {{ label('Free', '可用') }} {{ bytes(storage?.diskFreeBytes) }}
            </span>
          </div>
        </div>
      </article>
    </section>

    <div v-if="selected" class="drawer-mask" @click.self="selected = undefined">
      <aside class="api-drawer">
        <button class="drawer-close" @click="selected = undefined">×</button>
        <h2>{{ selected.apiCode }} · {{ selected.apiName }}</h2>
        <p>{{ apiDescription(selected) }}</p>
        <dl>
          <template v-for="(value, key) in selected" :key="key">
            <dt>{{ key }}</dt>
            <dd>{{ typeof value === 'object' && value !== null ? JSON.stringify(value) : value ?? '--' }}</dd>
          </template>
        </dl>
      </aside>
    </div>
  </main>
</template>
