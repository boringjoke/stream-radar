import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import {
  followLiveRoom,
  getLiveHome,
  unfollowLiveAnchor,
  unfollowLiveAnchors,
} from '@/api/live'
import { ApiRequestError } from '@/api/request'
import type { LiveAnchorCard, LiveFollowRequest } from '@/types/live'

type LiveHomeStatus = 'idle' | 'loading' | 'ready' | 'error'

/**
 * 用户首页主播数据状态。
 */
export const useLiveStore = defineStore('live', () => {
  const anchors = ref<LiveAnchorCard[]>([])
  const totalCount = ref(0)
  const liveCount = ref(0)
  const status = ref<LiveHomeStatus>('idle')
  const errorMessage = ref('')
  const isSubmitting = ref(false)

  const isLoading = computed(() => status.value === 'loading')
  const isReady = computed(() => status.value === 'ready')

  /**
   * 将 API 异常转换为用户可读提示。
   *
   * @param error 请求异常
   * @returns 用户可读提示
   */
  function getErrorMessage(error: unknown): string {
    if (error instanceof ApiRequestError) {
      return error.message
    }
    return '网络暂时不可用，请稍后重试'
  }

  /**
   * 从主播列表重新计算页面统计数据。
   */
  function refreshSummary() {
    totalCount.value = anchors.value.length
    liveCount.value = anchors.value.filter((anchor) => anchor.liveStatus === 'LIVE').length
  }

  /**
   * 查询当前用户关注主播。
   */
  async function loadHome(): Promise<void> {
    status.value = 'loading'
    errorMessage.value = ''
    try {
      const home = await getLiveHome()
      anchors.value = home.anchors
      totalCount.value = home.totalCount
      liveCount.value = home.liveCount
      status.value = 'ready'
    } catch (error) {
      status.value = 'error'
      errorMessage.value = getErrorMessage(error)
      throw error
    }
  }

  /**
   * 新增关注并将返回卡片加入首页列表。
   *
   * @param request 关注请求
   */
  async function follow(request: LiveFollowRequest): Promise<void> {
    isSubmitting.value = true
    errorMessage.value = ''
    try {
      const anchor = await followLiveRoom(request)
      anchors.value = [...anchors.value, anchor]
      refreshSummary()
      status.value = 'ready'
    } catch (error) {
      errorMessage.value = getErrorMessage(error)
      throw error
    } finally {
      isSubmitting.value = false
    }
  }

  /**
   * 取消单个关注并从首页列表移除卡片。
   *
   * @param followId 关注关系主键
   */
  async function unfollow(followId: number): Promise<void> {
    isSubmitting.value = true
    errorMessage.value = ''
    try {
      await unfollowLiveAnchor({ followId })
      anchors.value = anchors.value.filter((anchor) => anchor.followId !== followId)
      refreshSummary()
    } catch (error) {
      errorMessage.value = getErrorMessage(error)
      throw error
    } finally {
      isSubmitting.value = false
    }
  }

  /**
   * 批量取消关注并从首页列表移除卡片。
   *
   * @param followIds 关注关系主键列表
   */
  async function unfollowBatch(followIds: number[]): Promise<void> {
    isSubmitting.value = true
    errorMessage.value = ''
    try {
      await unfollowLiveAnchors({ followIds })
      const selected = new Set(followIds)
      anchors.value = anchors.value.filter((anchor) => !selected.has(anchor.followId))
      refreshSummary()
    } catch (error) {
      errorMessage.value = getErrorMessage(error)
      throw error
    } finally {
      isSubmitting.value = false
    }
  }

  /**
   * 清空用户首页主播数据。
   */
  function clear() {
    anchors.value = []
    totalCount.value = 0
    liveCount.value = 0
    status.value = 'idle'
    errorMessage.value = ''
    isSubmitting.value = false
  }

  return {
    anchors,
    clear,
    errorMessage,
    follow,
    getErrorMessage,
    isLoading,
    isReady,
    isSubmitting,
    liveCount,
    loadHome,
    status,
    totalCount,
    unfollow,
    unfollowBatch,
  }
})
