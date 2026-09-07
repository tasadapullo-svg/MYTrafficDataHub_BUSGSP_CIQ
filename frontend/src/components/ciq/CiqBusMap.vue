<script setup lang="ts">
import L from 'leaflet'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { CiqBusRealtimeEvent } from '../../api/ciqBusDashboardApi'

const props = defineProps<{
  events: CiqBusRealtimeEvent[]
  loading?: boolean
  error?: string
}>()
const emit = defineEmits<{ retry: [] }>()
const { t, locale } = useI18n()
const root = ref<HTMLElement>()
const mapEl = ref<HTMLElement>()
const full = ref(false)
let map: L.Map | undefined
let markerLayer: L.LayerGroup | undefined
let resizeTimer: number | undefined
let resizeObserver: ResizeObserver | undefined

const routeColors: Record<string, string> = {
  '160': '#00d9ff',
  '170': '#48a7ff',
  '170X': '#35e2a0',
  '950': '#9a7bff'
}

const validEvents = computed(() => props.events.filter(event => {
  const lat = event.latitude
  const lon = event.longitude
  return lat != null && lon != null && Number.isFinite(lat) && Number.isFinite(lon)
    && lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180 && !(lat === 0 && lon === 0)
}))

function esc(value: unknown) {
  return String(value ?? '').replace(/[&<>'"]/g, char => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;'
  }[char]!))
}

function format(value?: string) {
  return value
    ? new Intl.DateTimeFormat(locale.value === 'zh' ? 'zh-CN' : 'en-GB', {
        timeZone: 'Asia/Kuala_Lumpur', dateStyle: 'short', timeStyle: 'medium'
      }).format(new Date(value))
    : '--'
}

function popup(event: CiqBusRealtimeEvent) {
  const rows = ([
    [t('ciqBusPage.routeNo'), event.routeNo],
    [t('ciqBusPage.operatorCode'), event.operatorCode],
    [t('ciqBusPage.directionCode'), event.directionCode],
    [t('ciqBusPage.matchedEventId'), event.matchedEventId],
    [t('latitude'), event.latitude?.toFixed(6)],
    [t('longitude'), event.longitude?.toFixed(6)],
    [t('ciqBusPage.lastSeenTime'), event.lastSeenTime ? format(event.lastSeenTime) : undefined],
    [t('ciqBusPage.estimatedArrival'), event.estimatedArrival ? format(event.estimatedArrival) : undefined],
    [t('ciqBusPage.sourceStopCode'), event.sourceStopCode],
    [t('ciqBusPage.entryStopCode'), event.entryStopCode],
    [t('ciqBusPage.exitStopCode'), event.exitStopCode],
    [t('ciqBusPage.observationCount'), event.observationCount],
    [t('ciqBusPage.confidence'), event.confidence == null ? undefined : event.confidence.toFixed(3)]
  ] as Array<[string, unknown]>).filter(([, value]) => value !== undefined && value !== null && value !== '')
  return `<div class="vehicle-popup ciqbus-popup"><h3>${esc(t('ciqBusPage.realtimeVehicle'))}<em>${esc(event.routeNo)}</em></h3>${rows.map(([label, value]) => `<div class="popup-row"><span>${esc(label)}</span><b>${esc(value)}</b></div>`).join('')}</div>`
}

function icon(event: CiqBusRealtimeEvent) {
  const color = routeColors[event.routeNo] ?? '#00d9ff'
  const arrow = event.directionCode === 'SG_TO_JB' ? '↗' : event.directionCode === 'JB_TO_SG' ? '↙' : '•'
  return L.divIcon({
    className: 'ciqbus-marker-wrap',
    iconSize: [42, 42],
    iconAnchor: [21, 21],
    popupAnchor: [0, -20],
    html: `<div class="ciqbus-marker" style="--route-color:${color}"><strong>${esc(event.routeNo)}</strong><span>${arrow}</span></div>`
  })
}

function render(fit = false) {
  if (!map || !markerLayer) return
  markerLayer.clearLayers()
  const bounds: L.LatLngTuple[] = []
  for (const event of validEvents.value) {
    const point: L.LatLngTuple = [event.latitude!, event.longitude!]
    bounds.push(point)
    L.marker(point, { icon: icon(event) })
      .bindPopup(popup(event), { maxWidth: 380 })
      .bindTooltip(`${event.routeNo} · ${event.directionCode} · ${event.matchedEventId}`, { direction: 'top', opacity: .96 })
      .addTo(markerLayer)
  }
  if (fit && bounds.length) map.fitBounds(L.latLngBounds(bounds), { padding: [28, 28], maxZoom: 15 })
}

async function toggleFullscreen() {
  if (!root.value) return
  if (!document.fullscreenElement) await root.value.requestFullscreen()
  else await document.exitFullscreen()
}
function onFullscreen() {
  full.value = !!document.fullscreenElement
  scheduleResize()
}
function scheduleResize() {
  if (resizeTimer != null) window.clearTimeout(resizeTimer)
  resizeTimer = window.setTimeout(() => map?.invalidateSize(), 120)
}

onMounted(() => {
  map = L.map(mapEl.value!, { preferCanvas: true, zoomControl: true, attributionControl: true })
    .setView([1.4552, 103.7680], 14)
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19, attribution: '© OpenStreetMap'
  }).addTo(map)
  markerLayer = L.layerGroup().addTo(map)
  render(true)
  document.addEventListener('fullscreenchange', onFullscreen)
  window.addEventListener('resize', scheduleResize)
  resizeObserver = new ResizeObserver(scheduleResize)
  if (root.value) resizeObserver.observe(root.value)
  scheduleResize()
})

onBeforeUnmount(() => {
  document.removeEventListener('fullscreenchange', onFullscreen)
  window.removeEventListener('resize', scheduleResize)
  resizeObserver?.disconnect()
  if (resizeTimer != null) window.clearTimeout(resizeTimer)
  map?.remove()
})

watch(() => props.events, () => render(true), { deep: false })
watch(locale, async () => { await nextTick(); render(false) })
</script>

<template>
  <div ref="root" class="ciqbus-map-shell">
    <div ref="mapEl" class="ciqbus-map-canvas"></div>
    <div class="ciqbus-map-legend">
      <span v-for="route in ['160','170','170X','950']" :key="route" :style="{'--route-color': routeColors[route]}"><i></i>{{ route }}</span>
      <span>↗ SG_TO_JB</span><span>↙ JB_TO_SG</span>
    </div>
    <button class="fullscreen-btn" @click="toggleFullscreen">⛶ {{ full ? t('exitFullScreen') : t('fullScreen') }}</button>
    <div v-if="loading" class="panel-state">{{ t('ciqBusPage.loadingRealtime') }}</div>
    <div v-else-if="error" class="panel-state error">{{ error }}<button @click="emit('retry')">{{ t('retry') }}</button></div>
    <footer class="map-footer">
      <span>{{ t('ciqBusPage.mapArea') }}</span>
      <span>{{ t('ciqBusPage.mapVehicles') }}: <b>{{ validEvents.length }}</b></span>
    </footer>
  </div>
</template>
