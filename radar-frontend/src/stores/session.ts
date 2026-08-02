import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import type { SessionUser } from '@/types/session'

/**
 * 登录会话状态骨架。
 *
 * 账号阶段再接入 session 查询、登录和退出请求；当前不主动伪造登录状态。
 */
export const useSessionStore = defineStore('session', () => {
  const user = ref<SessionUser | null>(null)
  const isAuthenticated = computed(() => user.value !== null)

  function setUser(nextUser: SessionUser | null) {
    user.value = nextUser
  }

  function clear() {
    user.value = null
  }

  return {
    clear,
    isAuthenticated,
    setUser,
    user,
  }
})
