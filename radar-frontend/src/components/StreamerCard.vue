<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'

import CornerOrnaments from '@/components/CornerOrnaments.vue'
import GoldDivider from '@/components/GoldDivider.vue'
import PlatformWatermark from '@/components/PlatformWatermark.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { platformCatalog } from '@/types/platform'

export type StreamStatus = 'live' | 'offline' | 'unknown' | 'error'
export type PlatformCode = 'bilibili' | 'douyu' | 'huya' | 'douyin'

export interface StreamerCardData {
  /** 展示数据的稳定标识。 */
  id: number | string
  /** 主播展示名称。 */
  name: string
  /** 所属直播平台。 */
  platform: PlatformCode
  /** 项目内头像静态资源路径；缺省时使用首字母占位。 */
  avatarPath?: string | null
  /** 当前直播状态。 */
  status: StreamStatus
  /** 直播标题或最近一次直播说明。 */
  title: string
  /** 平台观看人数或人气值；未直播或不可用时为空。 */
  viewers?: string | null
  /** 直播间地址。 */
  url: string
}

interface Props {
  /** 卡片展示数据。 */
  streamer: StreamerCardData
  /** 延迟展示时间，仅用于页面进入动画。 */
  delay?: number
  /** 是否显示“进入直播间”链接。 */
  showAction?: boolean
  /** 是否允许当前用户管理关注关系。 */
  removable?: boolean
  /** 是否进入批量取消关注模式。 */
  batchMode?: boolean
  /** 当前卡片是否已被批量选中。 */
  selected?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  delay: 0,
  showAction: true,
  removable: false,
  batchMode: false,
  selected: false,
})

const emit = defineEmits<{
  /** 请求取消当前关注。 */
  remove: []
  /** 切换当前卡片的批量选择状态。 */
  toggleSelect: []
}>()

const hovered = ref(false)
const imageFailed = ref(false)
const platform = computed(() => platformCatalog[props.streamer.platform])
const isLive = computed(() => props.streamer.status === 'live')
const isError = computed(() => props.streamer.status === 'error')
const avatarInitial = computed(() => props.streamer.name.slice(0, 1))
const metricLabel = computed(() => (
  props.streamer.platform === 'douyu' || props.streamer.platform === 'huya'
    ? '热度'
    : '观看中'
))
const nameElement = ref<HTMLElement | null>(null)

const NAME_MAX_FONT_SIZE = 15
const NAME_MIN_FONT_SIZE = 9
let nameResizeObserver: ResizeObserver | null = null

/** 根据卡片实际宽度缩放主播名称，保持单行显示且避免省略号。 */
const fitStreamerName = () => {
  const element = nameElement.value
  if (!element) {
    return
  }

  element.style.fontSize = `${NAME_MAX_FONT_SIZE}px`

  const availableWidth = element.clientWidth
  const contentWidth = element.scrollWidth
  if (!availableWidth || !contentWidth || contentWidth <= availableWidth) {
    return
  }

  const fittedFontSize = Math.max(
    NAME_MIN_FONT_SIZE,
    NAME_MAX_FONT_SIZE * availableWidth / contentWidth,
  )
  element.style.fontSize = `${fittedFontSize.toFixed(2)}px`
}

watch(() => props.streamer.name, () => {
  void nextTick(fitStreamerName)
})

onMounted(() => {
  void nextTick(fitStreamerName)

  if (typeof ResizeObserver !== 'undefined' && nameElement.value) {
    nameResizeObserver = new ResizeObserver(fitStreamerName)
    nameResizeObserver.observe(nameElement.value)
  }

  void document.fonts?.ready.then(fitStreamerName)
})

onUnmounted(() => {
  nameResizeObserver?.disconnect()
  nameResizeObserver = null
})
</script>

<template>
  <article
    class="streamer-card-wrap animate-fade-in-up"
    :class="{ 'streamer-card-wrap--hovered': hovered }"
    :style="{ animationDelay: `${0.1 + delay}s` }"
    @mouseenter="hovered = true"
    @mouseleave="hovered = false"
  >
    <div class="streamer-card-shell">
      <div
        class="streamer-card"
        :class="{
        'streamer-card--live': isLive,
        'streamer-card--error': isError,
        'streamer-card--selected': selected,
        'streamer-card--hovered': hovered,
        }"
      >
        <div v-if="isLive" class="streamer-card__live-strip" :style="{ backgroundColor: platform.color }"></div>

        <CornerOrnaments :color="isLive ? (hovered ? 'var(--color-gold)' : 'var(--color-gold-border)') : 'rgba(200,169,110,0.18)'" :size="12" />
        <PlatformWatermark :platform="streamer.platform" :style="{ color: 'var(--color-gold)' }" />

        <div class="streamer-card__topline">
          <span class="platform-seal">
            <i :style="{ backgroundColor: platform.color }" aria-hidden="true"></i>
            <span>{{ platform.roman }}</span>
          </span>
          <div class="streamer-card__topline-right">
            <StatusBadge :status="streamer.status" />
          </div>
        </div>

        <div class="streamer-card__avatar-row">
          <div class="streamer-card__avatar-medallion" :class="{ 'streamer-card__avatar-medallion--live': isLive }">
            <div v-if="isLive" class="streamer-card__halo streamer-card__halo--first" aria-hidden="true"></div>
            <div v-if="isLive" class="streamer-card__halo streamer-card__halo--second" aria-hidden="true"></div>
            <div class="streamer-card__avatar" :class="{ 'streamer-card__avatar--error': isError }">
              <img
                v-if="streamer.avatarPath && !imageFailed"
                :src="streamer.avatarPath"
                :alt="streamer.name"
                width="76"
                height="76"
                referrerpolicy="no-referrer"
                @error="imageFailed = true"
              />
              <span v-else class="streamer-card__avatar-fallback" aria-hidden="true">{{ avatarInitial }}</span>
            </div>
          </div>
        </div>

        <div class="streamer-card__identity">
          <div ref="nameElement" class="streamer-card__name">{{ streamer.name }}</div>
          <div class="streamer-card__viewers">
            <span v-if="isLive && streamer.viewers" class="gold-flicker">✦ {{ streamer.viewers }} {{ metricLabel }} ✦</span>
          </div>
        </div>

        <div class="streamer-card__divider">
          <GoldDivider :opacity="hovered ? 0.9 : 0.5" />
        </div>

        <div class="streamer-card__title">
          <p v-if="isError" class="streamer-card__error-copy">
            链接解析失败<br />
            <span>请确认直播间地址是否有效</span>
          </p>
          <p v-else>{{ streamer.title || '—' }}</p>
        </div>

        <div class="streamer-card__bottom-rule" aria-hidden="true"></div>

        <div v-if="showAction" class="streamer-card__action-row">
          <a class="streamer-card__action" :href="streamer.url" target="_blank" rel="noopener noreferrer">
            进入直播间
          </a>
        </div>
      </div>

      <div v-if="batchMode || removable" class="streamer-card__management-control">
        <button
          v-if="batchMode"
          class="streamer-card__select"
          :class="{ 'streamer-card__select--selected': selected }"
          type="button"
          :aria-label="selected ? `取消选择${streamer.name}` : `选择${streamer.name}`"
          :aria-pressed="selected"
          @click.stop="emit('toggleSelect')"
        >
          <span v-if="selected" aria-hidden="true">✓</span>
        </button>
        <button
          v-else
          class="streamer-card__remove"
          type="button"
          :aria-label="`取消关注${streamer.name}`"
          @click.stop="emit('remove')"
        >
          ×
        </button>
      </div>
    </div>
  </article>
</template>
