import { createRouter, createWebHistory } from 'vue-router'

import { pinia } from '@/stores/pinia'
import { useSessionStore } from '@/stores/session'
import HomePage from '@/views/pages/HomePage.vue'
import LoginPage from '@/views/pages/LoginPage.vue'
import RegisterPage from '@/views/pages/RegisterPage.vue'
import UserCenterPage from '@/views/pages/UserCenterPage.vue'

declare module 'vue-router' {
  interface RouteMeta {
    /** 页面标题。 */
    title?: string
    /** 是否必须先完成登录。 */
    requiresAuth?: boolean
    /** 已登录用户是否应离开当前页面。 */
    guestOnly?: boolean
  }
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomePage,
      meta: { title: '观测台 · Stream Radar' },
    },
    {
      path: '/login',
      name: 'login',
      component: LoginPage,
      meta: { title: '登录 · Stream Radar', guestOnly: true },
    },
    {
      path: '/register',
      name: 'register',
      component: RegisterPage,
      meta: { title: '注册 · Stream Radar', guestOnly: true },
    },
    {
      path: '/user-center',
      name: 'user-center',
      component: UserCenterPage,
      meta: { title: '用户中心 · Stream Radar', requiresAuth: true },
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
})

router.beforeEach(async (to) => {
  const sessionStore = useSessionStore(pinia)
  await sessionStore.bootstrap()

  if (to.meta.requiresAuth && !sessionStore.isAuthenticated) {
    return {
      name: 'login',
      query: { redirect: to.fullPath },
    }
  }

  if (to.meta.guestOnly && sessionStore.isAuthenticated) {
    return { name: 'home' }
  }

  return true
})

router.afterEach((to) => {
  document.title = typeof to.meta.title === 'string' ? to.meta.title : 'Stream Radar'
})

export default router
