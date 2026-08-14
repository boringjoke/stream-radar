import { get, post } from './request'
import type {
  AdminOverview,
  AdminLiveAnchorPage,
  AdminLiveAnchorPageRequest,
} from '@/types/admin'

/**
 * 查询管理中心全平台及平台拆分统计。
 */
export function getAdminOverview(): Promise<AdminOverview> {
  return get<AdminOverview>('/admin/overview')
}

/**
 * 分页查询全部主播及当前启用普通用户的关注人数。
 *
 * @param request 管理员主播分页查询请求
 */
export function getAdminLiveAnchorPage(
  request: AdminLiveAnchorPageRequest = {},
): Promise<AdminLiveAnchorPage> {
  const pageNum = request.pageNum ?? 1
  const pageSize = request.pageSize ?? 20
  return post<AdminLiveAnchorPage>('/admin/liveAnchor/page', {
    pageNum,
    pageSize,
    platform: request.platform || null,
    anchorName: request.anchorName?.trim() || null,
    roomId: request.roomId?.trim() || null,
    minFollowerCount: request.minFollowerCount ?? null,
    maxFollowerCount: request.maxFollowerCount ?? null,
  })
}
