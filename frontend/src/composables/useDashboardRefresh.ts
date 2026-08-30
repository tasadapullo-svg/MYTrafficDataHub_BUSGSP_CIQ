import { onBeforeUnmount, onMounted } from 'vue'

export function useDashboardRefresh(refresh:()=>void, intervalMs=300_000) {
  let timer:number|undefined
  onMounted(()=> { timer=window.setInterval(refresh, intervalMs) })
  onBeforeUnmount(()=> { if(timer) window.clearInterval(timer) })
}
