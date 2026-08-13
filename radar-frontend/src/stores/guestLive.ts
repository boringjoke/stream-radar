import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import { ApiRequestError, getApiUrl } from '@/api/request'
import { getGuestLiveHome } from '@/api/live'
import type { GuestLiveAnchorCard, GuestLiveHome } from '@/types/live'

type GuestLiveHomeStatus = 'idle' | 'loading' | 'ready' | 'error'

/**
 * 游客首页真实演示主播数据状态。
 */
export const useGuestLiveStore = defineStore('guestLive', () => {
  const anchors = ref<GuestLiveAnchorCard[]>([])
  const totalCount = ref(0)
  const liveCount = ref(0)
  const status = ref<GuestLiveHomeStatus>('idle')
  const errorMessage = ref('')
  const eventsStatus = ref<'disconnected' | 'connecting' | 'connected' | 'error'>('disconnected')
  let eventSource: EventSource | null = null

  const isLoading = computed(() => status.value === 'loading')

  /**
   * 应用游客首页完整快照。
   *
   * @param home 游客首页快照
   */
  function applySnapshot(home: GuestLiveHome): void {
    anchors.value = home.anchors || []
    totalCount.value = home.totalCount
    liveCount.value = home.liveCount
    status.value = 'ready'
    errorMessage.value = ''
  }

  /**
   * 将 API 异常转换为游客首页提示。
   *
   * @param error 请求异常
   * @returns 用户可读提示
   */
  function getErrorMessage(error: unknown): string {
    if (error instanceof ApiRequestError) {
      return error.message
    }
    return '真实主播数据暂时不可用，请稍后重试'
  }

  /**
   * 查询游客首页真实演示主播。
   */
  async function loadHome(): Promise<void> {
    status.value = 'loading'
    errorMessage.value = ''
    try {
      applySnapshot(await getGuestLiveHome())
    } catch (error) {
      status.value = 'error'
      errorMessage.value = getErrorMessage(error)
      throw error
    }
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
   * 建立游客首页真实数据 SSE 连接。
   */
  function connectEvents(): void {
    disconnectEvents()
    if (typeof EventSource === 'undefined') {
      return
    }

    eventsStatus.value = 'connecting'
    const source = new EventSource(getApiUrl('/live/guestEvents'), { withCredentials: true })
    source.addEventListener('snapshot', (event) => {
      const home = parseEventData<GuestLiveHome>(event)
      if (home) {
        applySnapshot(home)
      }
    })
    source.onopen = () => {
      eventsStatus.value = 'connected'
    }
    source.onerror = () => {
      // EventSource 会依据服务端 retry 值自动重连，保留现有卡片等待下一次完整快照。
      eventsStatus.value = 'error'
    }
    eventSource = source
  }

  /**
   * 关闭游客首页 SSE 连接。
   */
  function disconnectEvents(): void {
    if (eventSource) {
      eventSource.close()
      eventSource = null
    }
    eventsStatus.value = 'disconnected'
  }

  /**
   * 清理游客首页状态。
   */
  function clear(): void {
    anchors.value = []
    totalCount.value = 0
    liveCount.value = 0
    status.value = 'idle'
    errorMessage.value = ''
    disconnectEvents()
  }

  return {
    anchors,
    clear,
    connectEvents,
    disconnectEvents,
    errorMessage,
    eventsStatus,
    isLoading,
    liveCount,
    loadHome,
    status,
    totalCount,
  }
})
