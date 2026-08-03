/**
 * 用户资料响应。
 */
export interface UserProfile {
  /** 用户主键。 */
  id: number
  /** 用户名，只读。 */
  username: string
  /** 用户昵称。 */
  nickname: string
  /** 用户邮箱，没有填写时为 null。 */
  email: string | null
  /** 项目内头像静态资源路径，没有选择时为 null。 */
  avatarPath: string | null
}

/**
 * 预置头像选项。
 */
export interface AvatarOption {
  /** 项目内头像静态资源路径。 */
  path: string
  /** 头像展示名称。 */
  name: string
}

/**
 * 用户资料更新请求。
 */
export interface ProfileUpdateRequest {
  /** 用户昵称。 */
  nickname: string
  /** 可选邮箱地址，传 null 时清空邮箱。 */
  email: string | null
  /** 项目内预置头像路径，传 null 时清空头像。 */
  avatarPath: string | null
}
