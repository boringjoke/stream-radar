import type { SessionUser } from './session'

/**
 * 认证 Session 响应。
 */
export interface AuthSession {
  /** 当前请求是否已经认证。 */
  authenticated: boolean
  /** 当前登录用户摘要，未认证时为 null。 */
  user: SessionUser | null
}

/**
 * 用户注册请求。
 */
export interface RegisterRequest {
  /** 用户名。 */
  username: string
  /** 登录密码。 */
  password: string
  /** 确认密码。 */
  confirmPassword: string
  /** 可选邮箱地址。 */
  email: string | null
}

/**
 * 用户登录请求。
 */
export interface LoginRequest {
  /** 登录用户名。 */
  username: string
  /** 登录密码。 */
  password: string
}
