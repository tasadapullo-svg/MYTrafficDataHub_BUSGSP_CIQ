<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { TrendPoint } from '../../api/dashboardApi'
echarts.use([LineChart,GridComponent,LegendComponent,TooltipComponent,CanvasRenderer])
const props=defineProps<{points:TrendPoint[]}>();const el=ref<HTMLElement>();const {locale}=useI18n();let chart:echarts.ECharts|undefined;const colors:Record<string,string>={JB:'#16d591',KUCHING:'#ffb144',KL:'#168cff',MELAKA:'#a777ff'}
function draw(){if(!chart)return;const times=[...new Set(props.points.map(p=>p.bucketTime))].sort();const codes=['JB','KUCHING','KL','MELAKA'];chart.setOption({animation:false,color:codes.map(c=>colors[c]),tooltip:{trigger:'axis'},legend:{right:8,top:0,textStyle:{color:'#9cb7d2'}},grid:{left:10,right:12,top:32,bottom:8,containLabel:true},xAxis:{type:'category',boundaryGap:false,data:times.map(v=>new Intl.DateTimeFormat(locale.value==='zh'?'zh-CN':'en-GB',{timeZone:'Asia/Kuala_Lumpur',hour:'2-digit',minute:'2-digit',hour12:false}).format(new Date(v))),axisLabel:{color:'#8aa8c3',fontSize:11,margin:9,hideOverlap:true},axisTick:{alignWithLabel:true,lineStyle:{color:'#1a5078'}},axisLine:{lineStyle:{color:'#1a5078'}}},yAxis:{type:'value',axisLabel:{color:'#7795b2',margin:8},splitLine:{lineStyle:{color:'rgba(40,96,133,.28)'}}},series:codes.map(code=>({name:code,type:'line',showSymbol:false,smooth:.25,lineStyle:{width:2},areaStyle:{opacity:.08},data:times.map(time=>props.points.find(p=>p.cityCode===code&&p.bucketTime===time)?.recordCount||0)}))})}
const resize=()=>chart?.resize();onMounted(()=>{chart=echarts.init(el.value!);draw();window.addEventListener('resize',resize)});onBeforeUnmount(()=>{window.removeEventListener('resize',resize);chart?.dispose()});watch(()=>props.points,draw);watch(locale,draw)
</script>
<template><div ref="el" class="trend-chart"></div></template>
