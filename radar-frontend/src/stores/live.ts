import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import { getApiUrl } from '@/api/request'
import {
  followLiveRoom,
  getLiveHome,
  unfollowLiveAnchor,
  unfollowLiveAnchors,
} from '@/api/live'
import { ApiRequestError } from '@/api/request'
import type { LiveAnchorCard, LiveFollowRequest, LiveHome } from '@/types/live'

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
  const eventsStatus = ref<'disconnected' | 'connecting' | 'connected' | 'error'>('disconnected')
  let eventSource: EventSource | null = null

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
   * 应用 SSE 初始首页快照。
   *
   * @param home 当前用户首页快照
   */
  function applySnapshot(home: LiveHome): void {
    anchors.value = home.anchors
    totalCount.value = home.totalCount
    liveCount.value = home.liveCount
    status.value = 'ready'
  }

  /**
   * 应用单个主播变化事件；未知关注关系不主动追加，避免取关后的迟到事件复活卡片。
   *
   * @param anchor 变化后的主播卡片
   */
  function applyAnchorUpdate(anchor: LiveAnchorCard): void {
    const index = anchors.value.findIndex((item) => item.followId === anchor.followId)
    if (index < 0) {
      return
    }
    const nextAnchors = [...anchors.value]
    nextAnchors[index] = anchor
    anchors.value = nextAnchors
    refreshSummary()
  }

  /**
   * 解析 SSE JSON 数据。
   *
   * @param event SSE 消息
   * @returns 解析后的数据，格式不正确时返回 null
   */
  function parseEventData<T>(event: Event): T | null {
    const data = (event as MessageEvent<string>).data
    if (typeof data !== 'string' || !data) {
      return null
    }
    try {
      return JSON.parse(data) as T
    } catch {
      return null
    }
  }

  /**
   * 建立当前用户直播变化 SSE 连接。
   *
   * 连接仅使用 HttpOnly Session Cookie 认证，不把登录凭证写入浏览器存储。
   */
  function connectEvents(): void {
    disconnectEvents()
    if (typeof EventSource === 'undefined') {
      return
    }

    eventsStatus.value = 'connecting'
    const source = new EventSource(getApiUrl('/live/events'), { withCredentials: true })
    source.addEventListener('snapshot', (event) => {
      const home = parseEventData<LiveHome>(event)
      if (home) {
        applySnapshot(home)
      }
    })
    source.addEventListener('streamer.updated', (event) => {
      const anchor = parseEventData<LiveAnchorCard>(event)
      if (anchor) {
        applyAnchorUpdate(anchor)
      }
    })
    source.onopen = () => {
      eventsStatus.value = 'connected'
    }
    source.onerror = () => {
      // EventSource 会依据服务端 retry 值自动重连，状态只用于调试和页面生命周期管理。
      eventsStatus.value = 'error'
    }
    eventSource = source
  }

  /**
   * 关闭当前用户直播变化 SSE 连接。
   */
  function disconnectEvents(): void {
    if (eventSource) {
      eventSource.close()
      eventSource = null
    }
    eventsStatus.value = 'disconnected'
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
    disconnectEvents()
  }

  return {
    anchors,
    clear,
    connectEvents,
    errorMessage,
    follow,
    getErrorMessage,
    isLoading,
    isReady,
    isSubmitting,
    eventsStatus,
    liveCount,
    loadHome,
    status,
    totalCount,
    disconnectEvents,
    unfollow,
    unfollowBatch,
  }
})
