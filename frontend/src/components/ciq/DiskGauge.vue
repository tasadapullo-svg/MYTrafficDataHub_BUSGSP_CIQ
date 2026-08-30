<script setup lang="ts">
import * as echarts from 'echarts';import {onBeforeUnmount,onMounted,ref,watch} from 'vue'
const props=defineProps<{used?:number}>();const el=ref<HTMLElement>();let chart:echarts.ECharts|undefined
function draw(){const used=props.used??0;chart?.setOption({animation:false,series:[{type:'pie',radius:['66%','88%'],silent:true,label:{show:false},data:[{value:used,itemStyle:{color:'#168cff'}},{value:Math.max(0,100-used),itemStyle:{color:'#143550'}}]}],graphic:[{type:'text',left:'center',top:'38%',style:{text:props.used==null?'--':`${used.toFixed(1)}%`,fill:'#e8f8ff',font:'700 18px Arial',textAlign:'center'}}]},true)}
onMounted(()=>{chart=echarts.init(el.value!);draw()});onBeforeUnmount(()=>chart?.dispose());watch(()=>props.used,draw)
</script><template><div ref="el" class="disk-gauge"></div></template>
