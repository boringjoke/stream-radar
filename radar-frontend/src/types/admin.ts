import type { PageResult } from './api'
import type { LivePlatformCode } from './live'

/**
 * 管理员主播分页查询请求。
 */
export interface AdminLiveAnchorPageRequest {
  /** 当前页码，从 1 开始。 */
  pageNum?: number
  /** 每页记录数，取值范围为 1～100。 */
  pageSize?: number
  /** 平台精确筛选条件。 */
  platform?: LivePlatformCode | ''
  /** 主播名称模糊筛选条件。 */
  anchorName?: string
  /** 房间号精确筛选条件。 */
  roomId?: string
  /** 关注人数区间最小值，包含边界。 */
  minFollowerCount?: number | null
  /** 关注人数区间最大值，包含边界。 */
  maxFollowerCount?: number | null
}

/**
 * 管理中心全平台及平台拆分统计。
 */
export interface AdminOverview {
  /** 启用普通用户数量，不包含管理员。 */
  userCount: number
  /** 纳入统计范围的主播数量。 */
  anchorCount: number
  /** 被启用普通用户关注的去重主播数量。 */
  followedAnchorCount: number
  /** 四个平台分别纳入统计的主播数量。 */
  platformAnchorCounts: Record<LivePlatformCode, number>
}

/**
 * 管理中心主播分页记录。
 */
export interface AdminLiveAnchor {
  /** 主播主键。 */
  anchorId: number
  /** 平台标识。 */
  platform: LivePlatformCode
  /** 平台直播间标识。 */
  roomId: string
  /** 规范化直播间地址。 */
  roomUrl: string
  /** 主播名称。 */
  anchorName: string | null
  /** 主播头像地址。 */
  avatarUrl: string | null
  /** 当前主播的去重关注用户数。 */
  followerCount: number
}

/**
 * 管理员主播分页响应。
 */
export type AdminLiveAnchorPage = PageResult<AdminLiveAnchor>
