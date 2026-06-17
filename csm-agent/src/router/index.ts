import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { TOKEN_KEY } from '@/api/request'

const routes: RouteRecordRaw[] = [
  { path: '/login', component: () => import('@/views/Login.vue'), meta: { public: true } },
  { path: '/', redirect: '/tickets' },
  { path: '/tickets', component: () => import('@/views/Tickets.vue') },
  { path: '/chat/:id', component: () => import('@/views/Chat.vue') },
  { path: '/:pathMatch(.*)*', redirect: '/tickets' }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  const hasToken = !!localStorage.getItem(TOKEN_KEY)
  if (to.meta.public) return hasToken && to.path === '/login' ? '/tickets' : true
  if (!hasToken) return { path: '/login' }
  return true
})

export default router
