<script setup lang="ts">
import * as echarts from 'echarts'
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type { CiqTrendPoint } from '../../api/ciqDashboardApi'

const props = defineProps<{
  points: CiqTrendPoint[]
  hours: number
}>()

const el = ref<HTMLElement>()
let chart: echarts.ECharts | undefined

const colors = ['#00d9ff', '#48a7ff', '#ff6686', '#f5b83d', '#8f72ff', '#35e2a0', '#71839d', '#e58fff']
const HALF_HOUR_MS = 30 * 60 * 1000

function bucketEpoch(value: string | number | Date) {
  const timestamp = value instanceof Date ? value.getTime() : typeof value === 'number' ? value : new Date(value).getTime()
  return Math.floor(timestamp / HALF_HOUR_MS) * HALF_HOUR_MS
}

function axisLabel(timestamp: number) {
  const format: Intl.DateTimeFormatOptions = props.hours <= 24
    ? { timeZone: 'Asia/Kuala_Lumpur', hour: '2-digit', minute: '2-digit', hour12: false }
    : { timeZone: 'Asia/Kuala_Lumpur', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false }
  return new Intl.DateTimeFormat('en-GB', format).format(new Date(timestamp))
}

function buildHalfHourTimeline() {
  const end = bucketEpoch(Date.now())
  const start = end - Math.max(3, props.hours) * 60 * 60 * 1000
  const timeline: number[] = []
  for (let current = start; current <= end; current += HALF_HOUR_MS) timeline.push(current)
  return timeline
}

function draw() {
  if (!chart) return

  const timeline = buildHalfHourTimeline()
  const apiCodes = [...new Set(props.points.map((point) => point.apiCode))]
  const lookup = new Map<string, number>()

  for (const point of props.points) {
    lookup.set(`${point.apiCode}|${bucketEpoch(point.bucketTime)}`, point.records)
  }

  chart.setOption({
    animation: false,
    color: colors,
    grid: { left: 58, right: 16, top: 34, bottom: 38 },
    legend: {
      top: 2,
      textStyle: { color: '#a9c9df', fontSize: 10 }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'line' }
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: timeline.map(axisLabel),
      axisLabel: {
        color: '#7897ad',
        interval: props.hours <= 3 ? 0 : 'auto',
        hideOverlap: true
      },
      axisTick: {
        alignWithLabel: true,
        interval: props.hours <= 3 ? 0 : 'auto'
      },
      axisLine: { lineStyle: { color: '#19415e' } }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#7897ad' },
      splitLine: { lineStyle: { color: '#102d44' } }
    },
    series: apiCodes.map((apiCode) => ({
      name: apiCode,
      type: 'line',
      smooth: false,
      connectNulls: false,
      showSymbol: props.hours <= 3,
      symbol: 'circle',
      symbolSize: 5,
      data: timeline.map((timestamp) => lookup.get(`${apiCode}|${timestamp}`) ?? null)
    }))
  }, true)
}

function resize() {
  chart?.resize()
}

onMounted(() => {
  chart = echarts.init(el.value!)
  draw()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  chart?.dispose()
})

watch(() => [props.points, props.hours] as const, draw, { deep: false })
</script>

<template>
  <div ref="el" class="ciq-trend-chart"></div>
</template>
