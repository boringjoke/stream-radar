<script setup lang="ts">
import { computed, onMounted, onUnmounted, watch } from 'vue'
import { storeToRefs } from 'pinia'

import GoldDivider from '@/components/GoldDivider.vue'
import SectionHeader from '@/components/SectionHeader.vue'
import StreamerCard, { type StreamerCardData } from '@/components/StreamerCard.vue'
import UserHomePage from '@/views/pages/UserHomePage.vue'
import { useGuestLiveStore } from '@/stores/guestLive'
import { useSessionStore } from '@/stores/session'
import { platformCatalog } from '@/types/platform'
import type { GuestLiveAnchorCard } from '@/types/live'

const sessionStore = useSessionStore()
const { isAuthenticated } = storeToRefs(sessionStore)
const guestLiveStore = useGuestLiveStore()
const {
  anchors: guestAnchors,
  errorMessage: guestErrorMessage,
  eventsStatus: guestEventsStatus,
  isLoading: guestIsLoading,
  status: guestStatus,
} = storeToRefs(guestLiveStore)

/**
 * 将游客首页后端状态转换为主播卡片状态。
 *
 * @param statusValue 后端统一状态
 * @returns 前端卡片状态
 */
function toCardStatus(statusValue: GuestLiveAnchorCard['liveStatus']): StreamerCardData['status'] {
  if (statusValue === 'LIVE') {
    return 'live'
  }
  if (statusValue === 'OFFLINE') {
    return 'offline'
  }
  if (statusValue === 'ERROR') {
    return 'error'
  }
  return 'unknown'
}

/**
 * 格式化平台观看人数或热度。
 *
 * @param count 平台返回的观看人数或热度
 * @returns 展示用平台指标
 */
function formatPlatformMetric(count: number | null): string | null {
  if (count === null || count < 0) {
    return null
  }
  if (count >= 10000) {
    return `${(count / 10000).toFixed(count >= 100000 ? 0 : 1)}万`
  }
  if (count >= 1000) {
    return `${(count / 1000).toFixed(1)}千`
  }
  return String(count)
}

/**
 * 将游客首页真实主播转换为现有视觉卡片数据。
 *
 * @param anchor 游客首页真实主播
 * @returns 视觉卡片数据
 */
function toStreamerCard(anchor: GuestLiveAnchorCard): StreamerCardData {
  const platform = {
    BILIBILI: 'bilibili',
    DOUYU: 'douyu',
    HUYA: 'huya',
    DOUYIN: 'douyin',
  }[anchor.platform] as StreamerCardData['platform']
  const status = toCardStatus(anchor.liveStatus)
  return {
    id: `${anchor.platform}-${anchor.roomId}`,
    name: anchor.anchorName?.trim() || `房间 ${anchor.roomId}`,
    platform,
    avatarPath: anchor.avatarUrl,
    status,
    title: anchor.liveTitle?.trim()
      || (status === 'unknown' ? '等待平台数据源返回主播资料' : '—'),
    viewers: formatPlatformMetric(anchor.onlineCount),
    url: anchor.roomUrl,
  }
}

const demoStreamers = computed(() => guestAnchors.value.map(toStreamerCard))

let guestPageStarted = false

/**
 * 启动游客首页真实数据和 SSE。
 */
async function startGuestLive() {
  if (guestPageStarted) {
    return
  }
  guestPageStarted = true
  try {
    await guestLiveStore.loadHome()
  } catch {
    // 初始请求失败时仍建立 SSE，等待服务端首个完整快照恢复页面。
  }
  if (!guestPageStarted || isAuthenticated.value) {
    return
  }
  guestLiveStore.connectEvents()
}

/**
 * 重试游客首页真实数据。
 */
async function retryGuestLive() {
  guestPageStarted = false
  await startGuestLive()
}

/**
 * 停止游客首页真实数据连接。
 */
function stopGuestLive() {
  if (!guestPageStarted) {
    return
  }
  guestPageStarted = false
  guestLiveStore.clear()
}

watch(isAuthenticated, (authenticated) => {
  if (authenticated) {
    stopGuestLive()
  } else {
    void startGuestLive()
  }
}, { immediate: true })

onMounted(() => {
  if (!isAuthenticated.value) {
    void startGuestLive()
  }
})

onUnmounted(stopGuestLive)
</script>

<template>
  <UserHomePage v-if="isAuthenticated" />

  <main v-else class="guest-home">
    <section class="guest-hero" aria-labelledby="guest-home-title">
      <div class="guest-hero__inner animate-fade-in-up">
        <div class="guest-hero__ornament">
          <GoldDivider />
        </div>

        <div class="guest-hero__kicker">Cross-Platform Stream Monitor</div>

        <h1 id="guest-home-title">你的专属直播雷达</h1>

        <p class="guest-hero__description">关注 B站、斗鱼、虎牙、抖音的主播，开播立刻知道</p>

        <div class="platform-pills" aria-label="支持的平台">
          <span v-for="(platform, key) in platformCatalog" :key="key" class="platform-pill">
            <i :style="{ backgroundColor: platform.color }" aria-hidden="true"></i>
            {{ platform.roman }}
          </span>
        </div>

        <RouterLink class="figma-button figma-button--hero" to="/login">登录开始使用</RouterLink>
      </div>
    </section>

    <section class="demo-section" aria-labelledby="demo-section-title">
      <SectionHeader>
        <span id="demo-section-title">平台示例</span>
      </SectionHeader>

      <div v-if="guestStatus === 'idle' || guestIsLoading" class="guest-home__data-state">
        <div class="guest-home__data-state-mark" aria-hidden="true">◌</div>
        <p>正在获取四个平台真实主播数据…</p>
      </div>

      <div v-else-if="guestStatus === 'error' && demoStreamers.length === 0" class="guest-home__data-state guest-home__data-state--error">
        <div class="guest-home__data-state-mark" aria-hidden="true">!</div>
        <h2>真实主播数据暂时无法加载</h2>
        <p>{{ guestErrorMessage || '请稍后重试' }}</p>
        <button class="guest-home__data-state-action figma-button" type="button" @click="retryGuestLive">重新加载</button>
      </div>

      <div v-else class="demo-card-grid">
        <div v-for="(streamer, index) in demoStreamers" :key="streamer.id" class="demo-card-column">
          <StreamerCard :streamer="streamer" :delay="index * 0.08" />
        </div>
      </div>

      <p v-if="guestEventsStatus === 'error' && demoStreamers.length > 0" class="guest-home__sync-hint">
        实时状态连接暂时中断，正在自动重连…
      </p>
    </section>
  </main>
</template>
