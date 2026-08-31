<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type { SystemStatus } from '../../api/dashboardApi'
const props=withDefaults(defineProps<{status?:SystemStatus;mode?:'bus'|'ciq'}>(),{mode:'bus'})
const {t,locale}=useI18n()
const now=ref(new Date()); const timer=window.setInterval(()=>now.value=new Date(),1000); onBeforeUnmount(()=>clearInterval(timer))
const statusText=computed(()=>{
  if(!props.status) return locale.value==='zh'?'系统状态检测中':'Checking system'
  if(props.status.status==='ERROR') return t('systemError')
  if(props.status.status==='WARNING') return t('systemWarning')
  return t('systemNormal')
})
const dateTime=computed(()=>new Intl.DateTimeFormat(locale.value==='zh'?'zh-CN':'en-GB',{timeZone:'Asia/Kuala_Lumpur',year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',second:'2-digit',hour12:false}).format(now.value).replaceAll('/','-'))
const title=computed(()=>props.mode==='ciq'?t('ciqTitle'):t('title'))
</script>
<template>
  <header class="dashboard-header">
    <div class="bus-mark" aria-hidden="true">▣</div>
    <div class="title-wrap"><span class="title-line"></span><h1>{{title}}</h1><span class="title-line"></span></div>
    <div class="header-meta"><div><span>{{dateTime}}</span><span class="system-state" :class="status?.status?.toLowerCase() || 'checking'"><i></i>{{statusText}}</span></div><div class="language-switch"><a href="https://maturity-kleenex-irregular.ngrok-free.dev/monitor_EN.html" target="_blank" rel="noopener noreferrer">JB Traffic Dashboard</a><RouterLink to="/" :class="{active:mode==='bus'}">{{t('busGps')}}</RouterLink><RouterLink to="/ciq" :class="{active:mode==='ciq'}">{{t('ciqData')}}</RouterLink><button :class="{active:locale==='en'}" @click="locale='en'">English</button><button :class="{active:locale==='zh'}" @click="locale='zh'">中文</button></div></div>
  </header>
</template>
