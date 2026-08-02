<script setup lang="ts">
import { computed, ref } from 'vue'

import CornerOrnaments from '@/components/CornerOrnaments.vue'
import GoldDivider from '@/components/GoldDivider.vue'
import PlatformWatermark from '@/components/PlatformWatermark.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { platformCatalog } from '@/types/platform'

export type StreamStatus = 'live' | 'offline' | 'error'
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
  /** 观看人数；未直播或不可用时为空。 */
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
}

const props = withDefaults(defineProps<Props>(), {
  delay: 0,
  showAction: true,
})

const hovered = ref(false)
const imageFailed = ref(false)
const platform = computed(() => platformCatalog[props.streamer.platform])
const isLive = computed(() => props.streamer.status === 'live')
const isError = computed(() => props.streamer.status === 'error')
const avatarInitial = computed(() => props.streamer.name.slice(0, 1))
</script>

<template>
  <article
    class="streamer-card-wrap animate-fade-in-up"
    :style="{ animationDelay: `${0.1 + delay}s` }"
    @mouseenter="hovered = true"
    @mouseleave="hovered = false"
  >
    <div
      class="streamer-card"
      :class="{
        'streamer-card--live': isLive,
        'streamer-card--error': isError,
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
        <StatusBadge :status="streamer.status" />
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
              @error="imageFailed = true"
            />
            <span v-else class="streamer-card__avatar-fallback" aria-hidden="true">{{ avatarInitial }}</span>
          </div>
        </div>
      </div>

      <div class="streamer-card__identity">
        <div class="streamer-card__name">{{ streamer.name }}</div>
        <div class="streamer-card__viewers">
          <span v-if="isLive && streamer.viewers" class="gold-flicker">✦ {{ streamer.viewers }} 观看中 ✦</span>
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
  </article>
</template>
