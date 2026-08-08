import type { PageResult } from './api'

/**
 * 管理员主播分页查询请求。
 */
export interface AdminLiveAnchorPageRequest {
  /** 当前页码，从 1 开始。 */
  pageNum?: number
  /** 每页记录数，取值范围为 1～100。 */
  pageSize?: number
}

/**
 * 管理中心主播分页记录。
 */
export interface AdminLiveAnchor {
  /** 主播主键。 */
  anchorId: number
  /** 平台标识。 */
  platform: string
  /** 平台直播间标识。 */
  roomId: string
  /** 平台主播用户标识。 */
  platformUid: string | null
  /** 规范化直播间地址。 */
  roomUrl: string
  /** 主播名称。 */
  anchorName: string | null
  /** 主播头像地址。 */
  avatarUrl: string | null
  /** 直播封面地址。 */
  coverUrl: string | null
  /** 当前或最后一次有效直播标题。 */
  liveTitle: string | null
  /** 当前观看人数或平台人气值。 */
  onlineCount: number | null
  /** 直播状态。 */
  liveStatus: string
  /** 最后检测时间。 */
  lastCheckTime: string | null
  /** 当前主播的去重关注用户数。 */
  followerCount: number
}

/**
 * 管理员主播分页响应。
 */
export type AdminLiveAnchorPage = PageResult<AdminLiveAnchor>
