/**
 * 当前登录会话中的用户摘要。
 */
export interface SessionUser {
  /** 用户主键。 */
  id: number
  /** 用户名。 */
  username: string
  /** 用户昵称。 */
  nickname: string
  /** 项目内头像静态资源路径，没有头像时为 null。 */
  avatarPath: string | null
}
