import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { TOKEN_KEY } from '@/api/request'

const routes: RouteRecordRaw[] = [
  { path: '/login', name: 'login', component: () => import('@/views/Login.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('@/layout/Layout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('@/views/Dashboard.vue'), meta: { title: '工作台' } },
      { path: 'tenant', component: () => import('@/views/Tenant.vue'), meta: { title: '租户接入' } },
      { path: 'account', component: () => import('@/views/Account.vue'), meta: { title: 'PC账号' } },
      { path: 'account/agent', component: () => import('@/views/AgentAccount.vue'), meta: { title: '客服账号' } },
      { path: 'customer', component: () => import('@/views/Customer.vue'), meta: { title: 'C端用户' } },
      { path: 'ticket', component: () => import('@/views/Ticket.vue'), meta: { title: '工单管理' } },
      { path: 'qa', component: () => import('@/views/Qa.vue'), meta: { title: 'QA知识库' } },
      { path: 'stats/ticket', component: () => import('@/views/StatsTicket.vue'), meta: { title: '工单统计' } },
      { path: 'stats/agent', component: () => import('@/views/StatsAgent.vue'), meta: { title: '客服统计' } },
      { path: 'system/menu', component: () => import('@/views/SystemMenu.vue'), meta: { title: '菜单管理' } },
      { path: 'system/role', component: () => import('@/views/SystemRole.vue'), meta: { title: '角色权限' } },
      { path: 'config', component: () => import('@/views/Config.vue'), meta: { title: '接入配置' } },
      { path: 'log', component: () => import('@/views/Log.vue'), meta: { title: '审计日志' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const hasToken = !!localStorage.getItem(TOKEN_KEY)
  if (to.meta.public) {
    return hasToken && to.path === '/login' ? '/' : true
  }
  if (!hasToken) {
    return { path: '/login', query: to.fullPath !== '/' ? { redirect: to.fullPath } : undefined }
  }
  return true
})

export default router
