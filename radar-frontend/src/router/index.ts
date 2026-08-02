import { createRouter, createWebHistory } from 'vue-router'

import HomePage from '@/views/pages/HomePage.vue'
import LoginPage from '@/views/pages/LoginPage.vue'
import RegisterPage from '@/views/pages/RegisterPage.vue'
import UserCenterPage from '@/views/pages/UserCenterPage.vue'

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
      meta: { title: '登录 · Stream Radar' },
    },
    {
      path: '/register',
      name: 'register',
      component: RegisterPage,
      meta: { title: '注册 · Stream Radar' },
    },
    {
      path: '/user-center',
      name: 'user-center',
      component: UserCenterPage,
      meta: { title: '用户中心 · Stream Radar' },
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
})

router.afterEach((to) => {
  document.title = typeof to.meta.title === 'string' ? to.meta.title : 'Stream Radar'
})

export default router
