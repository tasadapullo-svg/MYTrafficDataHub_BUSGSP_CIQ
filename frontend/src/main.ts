import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import i18n from './i18n'
import 'leaflet/dist/leaflet.css'
import './styles/dashboard.css'
import './styles/viewport-fill.css'
import './styles/route-map.css'
import './styles/ciq-dashboard.css'

createApp(App).use(router).use(i18n).mount('#app')
