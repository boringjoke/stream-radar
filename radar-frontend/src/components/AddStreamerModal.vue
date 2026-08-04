<script setup lang="ts">
import { computed, ref } from 'vue'

import CornerOrnaments from '@/components/CornerOrnaments.vue'
import { platformCatalog } from '@/types/platform'

type PlatformCode = keyof typeof platformCatalog

interface Props {
  /** 是否正在等待后端解析。 */
  submitting?: boolean
  /** 后端返回的添加错误。 */
  errorMessage?: string
}

const props = withDefaults(defineProps<Props>(), {
  submitting: false,
  errorMessage: '',
})

const emit = defineEmits<{
  /** 关闭添加主播弹窗。 */
  close: []
  /** 将原始 URL 交给后端解析。 */
  submit: [roomUrl: string]
}>()

const roomUrl = ref('')
const localError = ref('')

const platformHosts: Record<string, PlatformCode> = {
  'live.bilibili.com': 'bilibili',
  'm.live.bilibili.com': 'bilibili',
  'www.douyu.com': 'douyu',
  'douyu.com': 'douyu',
  'm.douyu.com': 'douyu',
  'www.huya.com': 'huya',
  'huya.com': 'huya',
  'm.huya.com': 'huya',
  'live.douyin.com': 'douyin',
}

const detectedPlatform = computed<PlatformCode | null>(() => {
  try {
    const host = new URL(roomUrl.value.trim()).hostname.toLowerCase()
    return platformHosts[host] || null
  } catch {
    return null
  }
})

const errorMessage = computed(() => localError.value || props.errorMessage)

/**
 * 提交原始直播间 URL，不在前端生成平台主键或主播主键。
 */
function handleSubmit() {
  const normalizedUrl = roomUrl.value.trim()
  localError.value = ''
  if (!normalizedUrl) {
    localError.value = '请输入直播间链接'
    return
  }
  emit('submit', normalizedUrl)
}

/**
 * 清理前端输入错误并重新提交。
 */
function handleInput() {
  localError.value = ''
}
</script>

<template>
  <div class="modal-backdrop" role="presentation" @click.self="emit('close')">
    <section
      class="add-streamer-modal"
      role="dialog"
      aria-modal="true"
      aria-labelledby="add-streamer-title"
    >
      <CornerOrnaments color="var(--color-gold)" :size="14" />

      <div class="add-streamer-modal__heading">
        <div class="add-streamer-modal__kicker">ADD STREAMER</div>
        <h2 id="add-streamer-title">添加关注主播</h2>
        <p>粘贴直播间链接，平台和主播身份由服务端确认</p>
      </div>

      <form class="add-streamer-form" novalidate @submit.prevent="handleSubmit">
        <label class="add-streamer-field">
          <span>直播间链接</span>
          <div class="add-streamer-input-wrap">
            <input
              v-model="roomUrl"
              type="url"
              placeholder="https://live.bilibili.com/..."
              autocomplete="off"
              autofocus
              :disabled="submitting"
              :aria-invalid="Boolean(errorMessage)"
              @input="handleInput"
            />
            <span v-if="detectedPlatform" class="add-streamer-platform-hint">
              <i :style="{ backgroundColor: platformCatalog[detectedPlatform].color }" aria-hidden="true"></i>
              {{ platformCatalog[detectedPlatform].roman }}
            </span>
          </div>
        </label>

        <p v-if="errorMessage" class="add-streamer-modal__error" role="alert">
          {{ errorMessage }}
        </p>

        <div class="add-streamer-modal__platforms" aria-label="支持的平台">
          <span
            v-for="(platform, key) in platformCatalog"
            :key="key"
            class="add-streamer-platform-chip"
          >
            <i :style="{ backgroundColor: platform.color }" aria-hidden="true"></i>
            {{ platform.roman }}
          </span>
        </div>

        <div class="add-streamer-modal__actions">
          <button class="add-streamer-modal__cancel" type="button" :disabled="submitting" @click="emit('close')">
            取消
          </button>
          <button class="add-streamer-modal__submit" type="submit" :disabled="submitting">
            {{ submitting ? '解析中…' : '确认添加' }}
          </button>
        </div>
      </form>
    </section>
  </div>
</template>
