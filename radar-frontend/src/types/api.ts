/**
 * 后端 JSON API 统一响应结构。
 *
 * @typeParam T 响应业务数据类型
 */
export interface ApiEnvelope<T> {
  /** 业务响应码，0 表示成功。 */
  code: number
  /** 面向调用方的响应说明。 */
  message: string
  /** 响应业务数据，无数据时为 null。 */
  data: T | null
}

/**
 * 通用分页响应数据。
 *
 * @typeParam T 分页记录类型
 */
export interface PageResult<T> {
  /** 当前页码，从 1 开始。 */
  pageNum: number
  /** 每页记录数。 */
  pageSize: number
  /** 符合查询条件的总记录数。 */
  total: number
  /** 当前页记录列表。 */
  records: T[]
}

/**
 * JSON 请求参数。
 */
export interface JsonRequestOptions extends Omit<RequestInit, 'body' | 'method'> {
  /** 当前基础封装支持的 HTTP 请求方法。 */
  method?: 'GET' | 'POST'
  /** POST 请求对象参数，禁止传入数组或原始值。 */
  body?: Record<string, unknown>
}
