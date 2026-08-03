import { get, post } from './request'
import type { AvatarOption, ProfileUpdateRequest, UserProfile } from '@/types/user'

/**
 * 查询当前登录用户资料。
 */
export function getProfile(): Promise<UserProfile> {
  return get<UserProfile>('/user/profile')
}

/**
 * 查询项目内置头像选项。
 */
export function getAvatarOptions(): Promise<AvatarOption[]> {
  return get<AvatarOption[]>('/user/avatarOptions')
}

/**
 * 更新当前登录用户资料。
 *
 * @param request 资料更新请求
 */
export function updateProfile(request: ProfileUpdateRequest): Promise<UserProfile> {
  return post<UserProfile>('/user/profile/update', {
    nickname: request.nickname,
    email: request.email,
    avatarPath: request.avatarPath,
  })
}
