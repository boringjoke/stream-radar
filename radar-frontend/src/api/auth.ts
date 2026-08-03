import { get, post } from './request'
import type { AuthSession, LoginRequest, RegisterRequest } from '@/types/auth'

/**
 * 初始化 CSRF Cookie。
 */
export function initializeCsrf(): Promise<null> {
  return get<null>('/auth/csrf')
}

/**
 * 查询当前认证 Session。
 */
export function getAuthSession(): Promise<AuthSession> {
  return get<AuthSession>('/auth/session')
}

/**
 * 注册用户并自动登录。
 *
 * @param request 注册请求
 */
export function register(request: RegisterRequest): Promise<AuthSession> {
  return post<AuthSession>('/auth/register', {
    username: request.username,
    password: request.password,
    confirmPassword: request.confirmPassword,
    email: request.email,
  })
}

/**
 * 登录用户。
 *
 * @param request 登录请求
 */
export function login(request: LoginRequest): Promise<AuthSession> {
  return post<AuthSession>('/auth/login', {
    username: request.username,
    password: request.password,
  })
}

/**
 * 退出当前登录 Session。
 */
export function logout(): Promise<null> {
  return post<null>('/auth/logout', {})
}
