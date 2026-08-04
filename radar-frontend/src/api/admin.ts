import { post } from './request'
import type {
  AdminLiveAnchorPage,
  AdminLiveAnchorPageRequest,
} from '@/types/admin'

/**
 * 分页查询当前仍被用户关注的主播及关注人数。
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
  })
}
