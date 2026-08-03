import type { ApiEnvelope, JsonRequestOptions } from '@/types/api'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

/**
 * 统一 API 请求异常。
 */
export class ApiRequestError extends Error {
  /** HTTP 状态码。 */
  readonly status: number

  /** 后端业务响应码。 */
  readonly code: number | undefined

  /** 后端返回的业务错误详情。 */
  readonly details: unknown

  constructor(message: string, status: number, code?: number, details?: unknown) {
    super(message)
    this.name = 'ApiRequestError'
    this.status = status
    this.code = code
    this.details = details
  }
}

/**
 * 发起统一 JSON 请求。
 *
 * 请求默认携带 Session Cookie，并尝试读取 Spring Security 常用的 CSRF Cookie。
 * 当前只提供 GET 和 POST 两种方法，POST 请求体必须是对象。
 */
export async function request<T>(path: string, options: JsonRequestOptions = {}): Promise<T> {
  const { body, method = 'GET', headers: initialHeaders, ...requestOptions } = options
  const headers = new Headers(initialHeaders)

  headers.set('Accept', 'application/json')

  if (body !== undefined) {
    if (method === 'GET') {
      throw new TypeError('GET 请求不能携带 JSON 请求体')
    }
    headers.set('Content-Type', 'application/json')
  }

  const csrfToken = readCookie('XSRF-TOKEN')
  if (csrfToken) {
    headers.set('X-XSRF-TOKEN', csrfToken)
  }

  const response = await fetch(joinUrl(path), {
    ...requestOptions,
    body: body === undefined ? undefined : JSON.stringify(body),
    credentials: 'include',
    headers,
    method,
  })

  if (response.status === 204) {
    return undefined as T
  }

  const responseText = await response.text()
  const payload = parseResponse<T>(responseText)

  if (!response.ok) {
    throw new ApiRequestError(
      payload?.message || `请求失败（${response.status}）`,
      response.status,
      payload?.code,
      payload?.data,
    )
  }

  if (payload && payload.code !== 0) {
    throw new ApiRequestError(
      payload.message || '请求处理失败',
      response.status,
      payload.code,
      payload.data,
    )
  }

  return payload?.data as T
}

/**
 * 发起 GET 请求。
 *
 * @param path API 相对路径
 * @param options 请求配置
 */
export function get<T>(
  path: string,
  options: Omit<JsonRequestOptions, 'body' | 'method'> = {},
): Promise<T> {
  return request<T>(path, { ...options, method: 'GET' })
}

/**
 * 发起 POST 请求。
 *
 * @param path API 相对路径
 * @param body 请求对象参数
 * @param options 请求配置
 */
export function post<T>(
  path: string,
  body: Record<string, unknown>,
  options: Omit<JsonRequestOptions, 'body' | 'method'> = {},
): Promise<T> {
  return request<T>(path, { ...options, body, method: 'POST' })
}

function joinUrl(path: string): string {
  const base = API_BASE_URL.replace(/\/$/, '')
  const suffix = path.startsWith('/') ? path : `/${path}`
  return `${base}${suffix}`
}

function parseResponse<T>(responseText: string): ApiEnvelope<T> | null {
  if (!responseText) {
    return null
  }

  try {
    return JSON.parse(responseText) as ApiEnvelope<T>
  } catch {
    return null
  }
}

function readCookie(name: string): string | undefined {
  if (typeof document === 'undefined') {
    return undefined
  }

  const cookie = document.cookie
    .split('; ')
    .find((item) => item.startsWith(`${name}=`))

  if (!cookie) {
    return undefined
  }

  return decodeURIComponent(cookie.slice(name.length + 1))
}
