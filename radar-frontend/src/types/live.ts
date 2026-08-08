/**
 * 后端直播平台标识。
 */
export type LivePlatformCode = 'BILIBILI' | 'DOUYU' | 'HUYA' | 'DOUYIN'

/**
 * 后端统一直播状态。
 */
export type LiveStatus = 'LIVE' | 'OFFLINE' | 'UNKNOWN' | 'ERROR'

/**
 * 用户首页主播卡片。
 */
export interface LiveAnchorCard {
  /** 关注关系主键。 */
  followId: number
  /** 主播主键。 */
  anchorId: number
  /** 平台标识。 */
  platform: LivePlatformCode
  /** 平台直播间标识。 */
  roomId: string
  /** 平台主播用户标识，当前阶段可能为空。 */
  platformUid: string | null
  /** 规范化直播间地址。 */
  roomUrl: string
  /** 主播名称，当前阶段可能为空。 */
  anchorName: string | null
  /** 主播头像地址，当前阶段可能为空。 */
  avatarUrl: string | null
  /** 直播封面地址，当前阶段可能为空。 */
  coverUrl: string | null
  /** 当前或最后一次有效直播标题，当前阶段可能为空。 */
  liveTitle: string | null
  /** 当前观看人数或平台人气值，当前阶段可能为空。 */
  onlineCount: number | null
  /** 统一直播状态。 */
  liveStatus: LiveStatus
  /** 最后检测时间，当前阶段可能为空。 */
  lastCheckTime: string | null
}

/**
 * 用户首页主播数据。
 */
export interface LiveHome {
  /** 当前用户关注主播总数。 */
  totalCount: number
  /** 当前状态为直播中的主播数量。 */
  liveCount: number
  /** 当前用户关注主播卡片列表。 */
  anchors: LiveAnchorCard[]
}

/**
 * 关注直播间请求。
 */
export interface LiveFollowRequest {
  /** 用户输入的直播间 URL。 */
  roomUrl: string
}

/**
 * 取消单个关注请求。
 */
export interface LiveUnfollowRequest {
  /** 关注关系主键。 */
  followId: number
}

/**
 * 批量取消关注请求。
 */
export interface LiveUnfollowBatchRequest {
  /** 关注关系主键列表。 */
  followIds: number[]
}
