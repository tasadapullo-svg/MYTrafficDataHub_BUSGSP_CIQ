import { createRouter, createWebHistory } from 'vue-router'

export default createRouter({
  history: createWebHistory('/dashboard/'),
  routes: [
    { path: '/', name: 'dashboard', component: () => import('./views/DashboardView.vue') },
    { path: '/ciq', name: 'ciq-dashboard', component: () => import('./views/CiqDashboardView.vue') },
    { path: '/ciqbus', name: 'ciqbus-dashboard', component: () => import('./views/CiqBusDashboardView.vue') }
  ]
})
