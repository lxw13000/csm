import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { TOKEN_KEY } from '@/api/request'

const routes: RouteRecordRaw[] = [
  { path: '/entry', component: () => import('@/views/Entry.vue'), meta: { public: true } },
  { path: '/', redirect: '/chat' },
  { path: '/chat', component: () => import('@/views/Chat.vue') },
  { path: '/:pathMatch(.*)*', redirect: '/chat' }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  const hasToken = !!localStorage.getItem(TOKEN_KEY)
  // 入口页可携带 app_id + token（业务 App 注入），始终放行
  if (to.meta.public) return true
  if (!hasToken) return { path: '/entry', query: to.query }
  return true
})

export default router
