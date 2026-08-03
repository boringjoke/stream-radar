import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import {
  getAuthSession,
  initializeCsrf,
  login as loginRequest,
  logout as logoutRequest,
  register as registerRequest,
} from '@/api/auth'
import { ApiRequestError } from '@/api/request'
import type { AuthSession, LoginRequest, RegisterRequest } from '@/types/auth'
import type { SessionUser } from '@/types/session'

type SessionStatus = 'idle' | 'loading' | 'authenticated' | 'unauthenticated'

/**
 * 登录会话状态。
 */
export const useSessionStore = defineStore('session', () => {
  const user = ref<SessionUser | null>(null)
  const status = ref<SessionStatus>('idle')
  const errorMessage = ref('')
  let bootstrapPromise: Promise<void> | null = null

  const isAuthenticated = computed(() => user.value !== null)
  const isLoading = computed(() => status.value === 'loading')
  const isInitialized = computed(() => status.value !== 'idle' && !isLoading.value)

  function setUser(nextUser: SessionUser | null) {
    user.value = nextUser
    status.value = nextUser ? 'authenticated' : 'unauthenticated'
  }

  function clear() {
    setUser(null)
  }

  /**
   * 从后端 Session 响应同步前端状态。
   *
   * @param session 后端认证 Session
   */
  function applySession(session: AuthSession) {
    user.value = session.authenticated ? session.user : null
    status.value = session.authenticated ? 'authenticated' : 'unauthenticated'
  }

  /**
   * 获取接口错误的用户可读提示。
   *
   * @param error 请求错误
   * @returns 用户可读错误提示
   */
  function getErrorMessage(error: unknown): string {
    if (error instanceof ApiRequestError) {
      return error.message
    }
    return '网络暂时不可用，请稍后重试'
  }

  /**
   * 初始化 CSRF 并恢复 Redis Session。
   */
  async function bootstrap(): Promise<void> {
    if (bootstrapPromise) {
      return bootstrapPromise
    }
    if (status.value !== 'idle') {
      return
    }

    bootstrapPromise = (async () => {
      status.value = 'loading'
      errorMessage.value = ''
      try {
        await initializeCsrf()
        applySession(await getAuthSession())
      } catch (error) {
        user.value = null
        status.value = 'unauthenticated'
        errorMessage.value = getErrorMessage(error)
      } finally {
        bootstrapPromise = null
      }
    })()

    return bootstrapPromise
  }

  /**
   * 注册并同步登录状态。
   *
   * @param request 注册请求
   */
  async function register(request: RegisterRequest): Promise<void> {
    status.value = 'loading'
    errorMessage.value = ''
    try {
      await initializeCsrf()
      applySession(await registerRequest(request))
    } catch (error) {
      status.value = 'unauthenticated'
      errorMessage.value = getErrorMessage(error)
      throw error
    }
  }

  /**
   * 登录并同步登录状态。
   *
   * @param request 登录请求
   */
  async function login(request: LoginRequest): Promise<void> {
    status.value = 'loading'
    errorMessage.value = ''
    try {
      await initializeCsrf()
      applySession(await loginRequest(request))
    } catch (error) {
      status.value = 'unauthenticated'
      errorMessage.value = getErrorMessage(error)
      throw error
    }
  }

  /**
   * 退出登录并清理前端会话状态。
   */
  async function logout(): Promise<void> {
    errorMessage.value = ''
    try {
      await initializeCsrf()
      await logoutRequest()
      clear()
    } catch (error) {
      errorMessage.value = getErrorMessage(error)
      throw error
    }
  }

  return {
    bootstrap,
    clear,
    errorMessage,
    isAuthenticated,
    isInitialized,
    isLoading,
    login,
    logout,
    register,
    setUser,
    status,
    user,
  }
})
