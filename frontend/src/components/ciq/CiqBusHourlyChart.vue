<script setup lang="ts">
import * as echarts from 'echarts'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { CiqBusHourlyPoint } from '../../api/ciqBusDashboardApi'

const props = defineProps<{ points: CiqBusHourlyPoint[] }>()
const { t, locale } = useI18n()
const el = ref<HTMLElement>()
let chart: echarts.ECharts | undefined
const hasData = computed(() => props.points.some(point => point.total > 0
  || point.sgToJb > 0 || point.jbToSg > 0 || point.avgCrossingSeconds != null))

function draw() {
  if (!chart) return
  chart.setOption({
    animation: false,
    color: ['#00d9ff', '#48a7ff', '#35e2a0', '#ffd06d', '#b58cff', '#ff8fa3', '#7ce0c3'],
    grid: { left: 58, right: 58, top: 76, bottom: 46 },
    legend: { top: 8, left: 'center', width: '94%', itemGap: 12, itemWidth: 18, itemHeight: 9, textStyle: { color: '#b5d0e3', fontSize: 13 } },
    tooltip: { trigger: 'axis', textStyle: { fontSize: 14 }, backgroundColor: 'rgba(3, 19, 35, .96)', borderColor: '#17618d' },
    xAxis: {
      type: 'category',
      data: props.points.map(p => p.label),
      axisLabel: { color: '#8caac0', interval: 1, fontSize: 12 },
      axisLine: { lineStyle: { color: '#19415e' } }
    },
    yAxis: [
      { type: 'value', name: t('ciqBusPage.crossingCount'), nameTextStyle: { color: '#8caac0', fontSize: 12 }, axisLabel: { color: '#8caac0', fontSize: 12 }, splitLine: { lineStyle: { color: '#102d44' } } },
      { type: 'value', name: t('ciqBusPage.minutes'), nameTextStyle: { color: '#8caac0', fontSize: 12 }, axisLabel: { color: '#8caac0', fontSize: 12 }, splitLine: { show: false } }
    ],
    series: [
      { name: 'SG_TO_JB', type: 'bar', data: props.points.map(p => p.sgToJb), barMaxWidth: 18 },
      { name: 'JB_TO_SG', type: 'bar', data: props.points.map(p => p.jbToSg), barMaxWidth: 18 },
      { name: 'TOTAL', type: 'bar', data: props.points.map(p => p.total), barMaxWidth: 18 },
      { name: t('ciqBusPage.avgCrossingTime'), type: 'line', yAxisIndex: 1, smooth: true, symbol: 'circle', symbolSize: 6, lineStyle: { width: 2 }, connectNulls: false, data: props.points.map(p => p.avgCrossingSeconds == null ? null : +(p.avgCrossingSeconds / 60).toFixed(2)) },
      { name: t('ciqBusPage.median'), type: 'line', yAxisIndex: 1, smooth: true, symbol: 'circle', symbolSize: 5, lineStyle: { width: 2 }, connectNulls: false, data: props.points.map(p => p.medianCrossingSeconds == null ? null : +(p.medianCrossingSeconds / 60).toFixed(2)) },
      { name: t('ciqBusPage.p90'), type: 'line', yAxisIndex: 1, smooth: true, symbol: 'circle', symbolSize: 5, lineStyle: { width: 2 }, connectNulls: false, data: props.points.map(p => p.p90CrossingSeconds == null ? null : +(p.p90CrossingSeconds / 60).toFixed(2)) },
      { name: t('ciqBusPage.p95'), type: 'line', yAxisIndex: 1, smooth: true, symbol: 'circle', symbolSize: 5, lineStyle: { width: 2 }, connectNulls: false, data: props.points.map(p => p.p95CrossingSeconds == null ? null : +(p.p95CrossingSeconds / 60).toFixed(2)) }
    ]
  }, true)
}

function resize() { chart?.resize() }
onMounted(() => { chart = echarts.init(el.value!); draw(); window.addEventListener('resize', resize) })
onBeforeUnmount(() => { window.removeEventListener('resize', resize); chart?.dispose() })
watch(() => props.points, draw, { deep: false })
watch(locale, draw)
</script>

<template>
  <div class="ciqbus-hourly-shell">
    <div ref="el" class="ciqbus-hourly-chart"></div>
    <div v-if="!hasData" class="ciqbus-chart-empty">{{ t('ciqBusPage.noData') }}</div>
  </div>
</template>
