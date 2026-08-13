import { get, post } from './request'
import type {
  LiveAnchorCard,
  LiveFollowRequest,
  LiveHome,
  GuestLiveHome,
  LiveUnfollowBatchRequest,
  LiveUnfollowRequest,
} from '@/types/live'

/**
 * 查询当前用户关注主播首页数据。
 */
export function getLiveHome(): Promise<LiveHome> {
  return get<LiveHome>('/live/home')
}

/**
 * 查询游客首页四个平台真实演示主播数据。
 */
export function getGuestLiveHome(): Promise<GuestLiveHome> {
  return get<GuestLiveHome>('/live/guestHome')
}

/**
 * 关注一个直播间。
 *
 * @param request 关注请求
 */
export function followLiveRoom(request: LiveFollowRequest): Promise<LiveAnchorCard> {
  return post<LiveAnchorCard>('/live/follow', {
    roomUrl: request.roomUrl,
  })
}

/**
 * 取消一个关注关系。
 *
 * @param request 取消关注请求
 */
export function unfollowLiveAnchor(request: LiveUnfollowRequest): Promise<null> {
  return post<null>('/live/unfollow', {
    followId: request.followId,
  })
}

/**
 * 批量取消关注关系。
 *
 * @param request 批量取消关注请求
 */
export function unfollowLiveAnchors(request: LiveUnfollowBatchRequest): Promise<null> {
  return post<null>('/live/unfollow/batch', {
    followIds: request.followIds,
  })
}
