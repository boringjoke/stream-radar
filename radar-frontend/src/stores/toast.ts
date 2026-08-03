import { ref } from 'vue'
import { defineStore } from 'pinia'

/** Toast 提示类型。 */
export type ToastType = 'success' | 'error' | 'info'

/**
 * 全局操作结果提示状态。
 */
export const useToastStore = defineStore('toast', () => {
  const visible = ref(false)
  const message = ref('')
  const type = ref<ToastType>('info')
  let hideTimer: ReturnType<typeof setTimeout> | null = null

  /** 清理当前 Toast 的自动关闭计时器。 */
  function clearHideTimer() {
    if (hideTimer !== null) {
      clearTimeout(hideTimer)
      hideTimer = null
    }
  }

  /**
   * 显示一条全局提示。
   *
   * @param nextMessage 提示内容
   * @param nextType 提示类型
   * @param duration 自动关闭时间，单位为毫秒
   */
  function show(
    nextMessage: string,
    nextType: ToastType = 'info',
    duration = 3600,
  ) {
    const normalizedMessage = nextMessage.trim()
    if (!normalizedMessage) {
      return
    }

    clearHideTimer()
    message.value = normalizedMessage
    type.value = nextType
    visible.value = true
    hideTimer = setTimeout(hide, duration)
  }

  /** 立即关闭当前全局提示。 */
  function hide() {
    clearHideTimer()
    visible.value = false
    message.value = ''
  }

  return {
    hide,
    message,
    show,
    type,
    visible,
  }
})
