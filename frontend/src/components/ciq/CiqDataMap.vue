<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import L from 'leaflet'
import type { CiqMapPoint } from '../../api/ciqDashboardApi'

const props = defineProps<{
  points: CiqMapPoint[]
  api: string
  loading: boolean
  error?: string
  language: 'en' | 'zh'
}>()
const emit = defineEmits<{ retry: []; 'api-change': [api: string] }>()

const root = ref<HTMLElement>()
const mapEl = ref<HTMLElement>()
const full = ref(false)
const heat = ref(false)
const heatRegions = ref(0)
let map: L.Map | undefined
let layer: L.LayerGroup | undefined

const label = (en: string, zh: string) => props.language === 'zh' ? zh : en
const esc = (v: unknown) => String(v ?? '--').replace(/[&<>'"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[c]!))
const time = (v?: string) => v
  ? new Intl.DateTimeFormat(props.language === 'zh' ? 'zh-CN' : 'en-GB', {
      timeZone: 'Asia/Kuala_Lumpur',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false
    }).format(new Date(v))
  : '--'

/**
 * LTA TrafficSpeedBands:
 * 1 = slowest, 8 = fastest/free-flow.
 * Slow traffic uses progressively deeper red; normal/free-flow uses green -> cyan -> blue.
 */
function color(p: CiqMapPoint) {
  if (p.apiCode === 'API02') return '#4f8fff'

  const band = Number(p.details['Speed Band'] ?? p.details.SpeedBand ?? 0)
  const speedBandColors: Record<number, string> = {
    1: '#7a0018', // very slow - deep red
    2: '#a50f2d',
    3: '#d7303f',
    4: '#f06b3f',
    5: '#35c56f', // normal
    6: '#19c99a',
    7: '#00bcd4',
    8: '#2787ff'  // free-flow - blue
  }
  return speedBandColors[band] ?? '#71839d'
}

function popup(p: CiqMapPoint) {
  const details = Object.entries(p.details || {})
    .map(([k, v]) => `<div class="popup-row"><span>${esc(k)}</span><b>${esc(v)}</b></div>`)
    .join('')

  return `<div class="ciq-map-popup">
    <h3>${esc(p.apiCode)} · ${esc(p.title)}</h3>
    <div class="popup-row"><span>${esc(label('Category', '类别'))}</span><b>${esc(p.category)}</b></div>
    <div class="popup-row"><span>${esc(label('Observed', '观测时间'))}</span><b>${esc(time(p.observedAt))}</b></div>
    <div class="popup-row"><span>${esc(label('Latitude', '纬度'))}</span><b>${p.latitude.toFixed(6)}</b></div>
    <div class="popup-row"><span>${esc(label('Longitude', '经度'))}</span><b>${p.longitude.toFixed(6)}</b></div>
    ${details}
  </div>`
}

const arrows: Record<string, string> = { N: '↑', NE: '↗', E: '→', SE: '↘', S: '↓', SW: '↙', W: '←', NW: '↖' }

function directionTip(p: CiqMapPoint) {
  const direction = String(p.details?.Direction ?? '')
  const bearing = p.details?.['Bearing Degrees']
  const location = p.apiCode === 'API02'
    ? label('Area reference point', '区域参考点')
    : direction
      ? `${arrows[direction] ?? '•'} ${direction}${bearing == null ? '' : ` · ${bearing}°`}`
      : label('Direction unavailable', '方位暂无')

  return `<div><b>${esc(p.apiCode)} · ${esc(p.title)}</b><span>${esc(label('Direction', '方位'))}: ${esc(location)}</span></div>`
}

function markerLayer(bounds: L.LatLngTuple[]) {
  for (const p of props.points) {
    if (!Number.isFinite(p.latitude) || !Number.isFinite(p.longitude)) continue

    const pos: L.LatLngTuple = [p.latitude, p.longitude]
    const c = color(p)
    const base = p.apiCode === 'API02' ? 8 : 5
    bounds.push(pos)

    const marker = L.circleMarker(pos, {
      radius: base,
      color: '#e9fcff',
      weight: 1,
      fillColor: c,
      fillOpacity: .9
    })
      .bindTooltip(directionTip(p), {
        direction: 'top',
        opacity: 1,
        className: 'ciq-direction-tooltip',
        offset: [0, -4]
      })
      .bindPopup(popup(p), { maxWidth: 420 })
      .addTo(layer!)

    marker.on('mouseover', () => {
      marker.setStyle({ radius: base + 5, weight: 3, fillOpacity: 1 })
      marker.bringToFront()
      marker.openTooltip()
    })
    marker.on('mouseout', () => marker.setStyle({ radius: base, weight: 1, fillOpacity: .9 }))
  }
}

function heatColor(v: number) {
  if (v > .75) return '#ff3d52'
  if (v > .5) return '#ff8b38'
  if (v > .25) return '#ffe04b'
  return '#00d9ff'
}

function heatLayer(bounds: L.LatLngTuple[]) {
  const cell = .012
  const bins = new Map<string, { lat: number; lon: number; count: number; api01: number; api02: number }>()

  for (const p of props.points) {
    if (!Number.isFinite(p.latitude) || !Number.isFinite(p.longitude)) continue

    bounds.push([p.latitude, p.longitude])
    const key = `${Math.floor(p.latitude / cell)}:${Math.floor(p.longitude / cell)}`
    const bin = bins.get(key) ?? { lat: 0, lon: 0, count: 0, api01: 0, api02: 0 }
    bin.lat += p.latitude
    bin.lon += p.longitude
    bin.count++
    p.apiCode === 'API01' ? bin.api01++ : bin.api02++
    bins.set(key, bin)
  }

  heatRegions.value = bins.size
  const max = Math.max(1, ...[...bins.values()].map(x => x.count))

  for (const b of bins.values()) {
    const ratio = b.count / max
    const pos: L.LatLngTuple = [b.lat / b.count, b.lon / b.count]
    const heatColorValue = heatColor(ratio)

    L.circleMarker(pos, {
      radius: 13 + 28 * Math.sqrt(ratio),
      color: heatColorValue,
      weight: 1.5,
      fillColor: heatColorValue,
      fillOpacity: .25 + .48 * ratio
    })
      .bindTooltip(
        `<div><b>${esc(label('Area heat', '区域热度'))} ${b.count.toLocaleString()} ${esc(label('points', '点'))}</b><span>API01 ${b.api01} · API02 ${b.api02}</span></div>`,
        { direction: 'top', className: 'ciq-direction-tooltip', opacity: 1 }
      )
      .bindPopup(
        `<div class="ciq-map-popup"><h3>${esc(label('Regional Heat Aggregation', '区域热力聚合'))}</h3><div class="popup-row"><span>${esc(label('Data Points', '数据点'))}</span><b>${b.count}</b></div><div class="popup-row"><span>API01</span><b>${b.api01}</b></div><div class="popup-row"><span>API02</span><b>${b.api02}</b></div></div>`
      )
      .addTo(layer!)
  }
}

function render(fit = true) {
  if (!map || !layer) return

  layer.clearLayers()
  const bounds: L.LatLngTuple[] = []
  heat.value ? heatLayer(bounds) : markerLayer(bounds)
  if (!heat.value) heatRegions.value = 0

  if (fit && bounds.length) {
    map.fitBounds(L.latLngBounds(bounds), { padding: [24, 24], maxZoom: 13 })
  }
}

function toggleHeat() {
  heat.value = !heat.value
  render(false)
}

async function toggleFullscreen() {
  if (!root.value) return
  if (!document.fullscreenElement) await root.value.requestFullscreen()
  else await document.exitFullscreen()
}

function onFullscreen() {
  full.value = !!document.fullscreenElement
  setTimeout(() => map?.invalidateSize(), 120)
}

onMounted(() => {
  map = L.map(mapEl.value!, { zoomControl: true }).setView([1.39, 103.77], 10)
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '© OpenStreetMap'
  }).addTo(map)
  layer = L.layerGroup().addTo(map)
  render()
  document.addEventListener('fullscreenchange', onFullscreen)
})

onBeforeUnmount(() => {
  document.removeEventListener('fullscreenchange', onFullscreen)
  map?.remove()
})

watch(() => props.points, async () => {
  await nextTick()
  map?.invalidateSize()
  render()
}, { deep: false })

// Leaflet popup/tooltip content is generated as HTML, so rebuild it when language changes.
watch(() => props.language, async () => {
  await nextTick()
  render(false)
})
</script>

<template>
  <div ref="root" class="ciq-map-shell">
    <div ref="mapEl" class="ciq-map-canvas"></div>

    <div class="ciq-map-toolbar">
      <select :value="api" @change="emit('api-change', ($event.target as HTMLSelectElement).value)">
        <option value="ALL">API01 + API02</option>
        <option value="API01">API01 TrafficSpeedBands</option>
        <option value="API02">API02 Estimated Travel Times</option>
      </select>

      <button class="heatmap-btn" :class="{ active: heat }" @click="toggleHeat">
        {{ heat ? label('● Point View', '● 点位图') : label('◉ Area Heatmap', '◉ 区域热力图') }}
      </button>

      <span><i class="api01-dot"></i>{{ label('API01 Road-segment Midpoints', 'API01 路段中点') }}</span>
      <span><i class="api02-dot"></i>{{ label('API02 Study Areas', 'API02 研究区域') }}</span>
    </div>

    <button class="fullscreen-btn" @click="toggleFullscreen">
      ⛶ {{ full ? label('Exit Full Screen', '退出全屏') : label('Full Screen Map', '全屏地图') }}
    </button>

    <div v-if="loading" class="panel-state">{{ label('Loading map data…', '地图数据加载中…') }}</div>
    <div v-else-if="error" class="panel-state error">
      {{ error }}
      <button @click="emit('retry')">{{ label('Retry', '重试') }}</button>
    </div>

    <footer class="ciq-map-footer">
      {{ heat
        ? label(`Area heatmap · ${heatRegions} regions · click for aggregation details`, `区域热力图 ${heatRegions} 个区域 · 点击查看聚合详情`)
        : label(`Real points ${points.length.toLocaleString()} · hover for direction · click for details`, `真实点位 ${points.length.toLocaleString()} · 悬停查看方位，点击查看详情`) }}
    </footer>
  </div>
</template>
