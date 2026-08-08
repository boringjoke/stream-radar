<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute, useRouter } from 'vue-router'

import { ApiRequestError } from '@/api/request'
import AddStreamerModal from '@/components/AddStreamerModal.vue'
import ConfirmModal from '@/components/ConfirmModal.vue'
import CornerOrnaments from '@/components/CornerOrnaments.vue'
import SectionHeader from '@/components/SectionHeader.vue'
import StreamerCard, { type PlatformCode, type StreamerCardData } from '@/components/StreamerCard.vue'
import { useLiveStore } from '@/stores/live'
import { useToastStore } from '@/stores/toast'
import { platformCatalog } from '@/types/platform'
import type { LiveAnchorCard, LivePlatformCode } from '@/types/live'

type PlatformFilter = 'all' | PlatformCode
type StatusFilter = 'all' | 'live' | 'offline'

const PLATFORM_MAP: Record<LivePlatformCode, PlatformCode> = {
  BILIBILI: 'bilibili',
  DOUYU: 'douyu',
  HUYA: 'huya',
  DOUYIN: 'douyin',
}

const router = useRouter()
const route = useRoute()
const liveStore = useLiveStore()
const toastStore = useToastStore()
const {
  anchors,
  errorMessage,
  isLoading,
  isReady,
  isSubmitting,
  liveCount,
  status,
  totalCount,
} = storeToRefs(liveStore)

const platformFilter = ref<PlatformFilter>('all')
const statusFilter = ref<StatusFilter>('all')
const batchMode = ref(false)
const selectedFollowIds = ref<Set<number>>(new Set())
const showAddModal = ref(false)
const addErrorMessage = ref('')
const pendingUnfollow = ref<StreamerCardData | null>(null)
const pendingBatchUnfollow = ref(false)

const filteredAnchors = computed(() => anchors.value.filter((anchor) => {
  const platformMatched = platformFilter.value === 'all'
    || PLATFORM_MAP[anchor.platform] === platformFilter.value
  const statusMatched = statusFilter.value === 'all'
    || (statusFilter.value === 'live' && anchor.liveStatus === 'LIVE')
    || (statusFilter.value === 'offline' && anchor.liveStatus === 'OFFLINE')
  return platformMatched && statusMatched
}))

const filteredCards = computed(() => filteredAnchors.value
  .map((anchor, index) => ({
    card: toStreamerCard(anchor),
    index,
  }))
  .sort((left, right) => {
    const liveRank = Number(right.card.status === 'live') - Number(left.card.status === 'live')
    return liveRank || left.index - right.index
  })
  .map(({ card }) => card))
const selectedCount = computed(() => selectedFollowIds.value.size)
const allVisibleSelected = computed(() => (
  filteredCards.value.length > 0
  && filteredCards.value.every((card) => selectedFollowIds.value.has(Number(card.id)))
))

const platformFilterOptions: Array<{ key: PlatformFilter; label: string }> = [
  { key: 'all', label: '全部' },
  { key: 'bilibili', label: 'BILIBILI' },
  { key: 'douyu', label: 'DOUYU' },
  { key: 'huya', label: 'HUYA' },
  { key: 'douyin', label: 'DOUYIN' },
]

const statusFilterOptions: Array<{ key: StatusFilter; label: string }> = [
  { key: 'all', label: '全部状态' },
  { key: 'live', label: '直播中' },
  { key: 'offline', label: '未直播' },
]

/**
 * 将后端状态转换为主播卡片状态。
 *
 * @param statusValue 后端统一状态
 * @returns 前端卡片状态
 */
function toCardStatus(statusValue: LiveAnchorCard['liveStatus']): StreamerCardData['status'] {
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
 * 格式化平台观看人数或热度，只有直播中状态的卡片会展示该字段。
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
 * 将后端主播卡片转换为现有视觉卡片数据。
 *
 * @param anchor 后端主播卡片
 * @returns 前端展示卡片
 */
function toStreamerCard(anchor: LiveAnchorCard): StreamerCardData {
  const platform = PLATFORM_MAP[anchor.platform]
  const statusValue = toCardStatus(anchor.liveStatus)
  return {
    id: anchor.followId,
    name: anchor.anchorName?.trim() || `房间 ${anchor.roomId}`,
    platform,
    avatarPath: anchor.avatarUrl,
    status: statusValue,
    title: anchor.liveTitle?.trim()
      || (statusValue === 'unknown' ? '等待平台数据源返回主播资料' : '—'),
    viewers: formatPlatformMetric(anchor.onlineCount),
    url: anchor.roomUrl,
  }
}

/**
 * 将请求异常转换为当前页面提示。
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
 * 处理 Session 失效并回到登录页。
 *
 * @param error 请求异常
 * @returns 是否已完成登录跳转
 */
async function redirectWhenUnauthorized(error: unknown): Promise<boolean> {
  if (!(error instanceof ApiRequestError) || error.status !== 401) {
    return false
  }
  await router.replace({
    name: 'login',
    query: { redirect: route.fullPath },
  })
  return true
}

/**
 * 查询当前用户首页数据。
 */
async function loadHome() {
  try {
    await liveStore.loadHome()
    liveStore.connectEvents()
  } catch (error) {
    if (!(await redirectWhenUnauthorized(error))) {
      toastStore.show(errorMessage.value || getErrorMessage(error), 'error')
    }
  }
}

/**
 * 打开添加主播弹窗。
 */
function openAddModal() {
  addErrorMessage.value = ''
  showAddModal.value = true
}

/**
 * 提交直播间链接并建立关注关系。
 *
 * @param roomUrl 用户输入的原始直播间链接
 */
async function handleFollow(roomUrl: string) {
  addErrorMessage.value = ''
  try {
    await liveStore.follow({ roomUrl })
    showAddModal.value = false
    toastStore.show('主播已添加到关注列表', 'success')
  } catch (error) {
    if (await redirectWhenUnauthorized(error)) {
      return
    }
    const message = getErrorMessage(error)
    addErrorMessage.value = message
    toastStore.show(message, 'error')
  }
}

/**
 * 请求取消单个关注，确认后再调用后端。
 *
 * @param card 待取消的主播卡片
 */
function requestUnfollow(card: StreamerCardData) {
  pendingUnfollow.value = card
}

/**
 * 确认取消单个关注。
 */
async function confirmUnfollow() {
  const followId = Number(pendingUnfollow.value?.id)
  pendingUnfollow.value = null
  if (!followId) {
    return
  }
  try {
    await liveStore.unfollow(followId)
    toastStore.show('已取消关注', 'success')
  } catch (error) {
    if (!(await redirectWhenUnauthorized(error))) {
      toastStore.show(getErrorMessage(error), 'error')
    }
  }
}

/**
 * 切换批量模式并清理已选关系。
 */
function toggleBatchMode() {
  batchMode.value = !batchMode.value
  selectedFollowIds.value = new Set()
}

/**
 * 退出批量模式并清理选择。
 */
function exitBatchMode() {
  batchMode.value = false
  selectedFollowIds.value = new Set()
}

/**
 * 切换单张卡片的批量选择状态。
 *
 * @param followId 关注关系主键
 */
function toggleSelected(followId: number) {
  const next = new Set(selectedFollowIds.value)
  if (next.has(followId)) {
    next.delete(followId)
  } else {
    next.add(followId)
  }
  selectedFollowIds.value = next
}

/**
 * 全选或取消全选当前筛选结果。
 */
function toggleSelectAll() {
  const next = new Set(selectedFollowIds.value)
  if (allVisibleSelected.value) {
    filteredCards.value.forEach((card) => next.delete(Number(card.id)))
  } else {
    filteredCards.value.forEach((card) => next.add(Number(card.id)))
  }
  selectedFollowIds.value = next
}

/**
 * 打开批量取消确认弹窗。
 */
function requestBatchUnfollow() {
  if (selectedCount.value > 0) {
    pendingBatchUnfollow.value = true
  }
}

/**
 * 确认批量取消关注。
 */
async function confirmBatchUnfollow() {
  const followIds = Array.from(selectedFollowIds.value)
  pendingBatchUnfollow.value = false
  if (followIds.length === 0) {
    return
  }
  try {
    await liveStore.unfollowBatch(followIds)
    exitBatchMode()
    toastStore.show(`已取消关注 ${followIds.length} 位主播`, 'success')
  } catch (error) {
    if (!(await redirectWhenUnauthorized(error))) {
      toastStore.show(getErrorMessage(error), 'error')
    }
  }
}

/**
 * 清除当前平台和状态筛选。
 */
function clearFilters() {
  platformFilter.value = 'all'
  statusFilter.value = 'all'
}

watch([platformFilter, statusFilter], () => {
  selectedFollowIds.value = new Set()
})

onMounted(loadHome)
onUnmounted(liveStore.disconnectEvents)
</script>

<template>
  <main class="user-home">
    <div class="user-home__inner">
      <div class="user-home__heading-row animate-fade-in-up">
        <div>
          <div class="user-home__kicker">MY STREAMERS</div>
          <h1 id="user-home-title">关注主播</h1>
        </div>

        <div class="user-home__summary" aria-label="关注统计">
          <div v-if="liveCount > 0" class="user-home__live-count">
            <i aria-hidden="true"></i>
            <strong>{{ liveCount }}</strong>
            <span>位直播中</span>
          </div>
          <div class="user-home__count">
            <strong>{{ totalCount }}</strong>
            <span>位关注</span>
          </div>
        </div>
      </div>

      <div class="user-home__toolbar">
        <div class="user-home__toolbar-actions">
          <template v-if="!batchMode">
            <button class="user-home__tool" type="button" :disabled="!isReady || totalCount === 0" @click="toggleBatchMode">
              批量取关
            </button>
            <button class="user-home__tool user-home__tool--primary" type="button" @click="openAddModal">
              + 添加主播
            </button>
          </template>
          <template v-else>
            <span class="user-home__selected-count">已选 {{ selectedCount }} 位</span>
            <button class="user-home__tool" type="button" :disabled="filteredCards.length === 0" @click="toggleSelectAll">
              {{ allVisibleSelected ? '取消全选' : '全选当前' }}
            </button>
            <button class="user-home__tool user-home__tool--danger" type="button" :disabled="selectedCount === 0 || isSubmitting" @click="requestBatchUnfollow">
              取关选中 ({{ selectedCount }})
            </button>
            <button class="user-home__tool" type="button" @click="exitBatchMode">退出</button>
          </template>
        </div>
      </div>

      <div class="user-home__filters" aria-label="主播筛选">
        <div class="user-home__filter-row">
          <button
            v-for="option in platformFilterOptions"
            :key="option.key"
            class="user-home__filter"
            :class="{ 'user-home__filter--active': platformFilter === option.key }"
            type="button"
            @click="platformFilter = option.key"
          >
            <i v-if="option.key !== 'all'" :style="{ backgroundColor: platformCatalog[option.key].color }" aria-hidden="true"></i>
            {{ option.label }}
            <small>{{ option.key === 'all' ? anchors.length : anchors.filter((anchor) => PLATFORM_MAP[anchor.platform] === option.key).length }}</small>
          </button>
        </div>
        <div class="user-home__filter-row user-home__filter-row--status">
          <button
            v-for="option in statusFilterOptions"
            :key="option.key"
            class="user-home__filter"
            :class="{ 'user-home__filter--active': statusFilter === option.key }"
            type="button"
            @click="statusFilter = option.key"
          >
            <i v-if="option.key !== 'all'" :class="`user-home__status-dot user-home__status-dot--${option.key}`" aria-hidden="true"></i>
            {{ option.label }}
            <small>
              {{ option.key === 'all'
                ? anchors.length
                : anchors.filter((anchor) => option.key === 'live'
                  ? anchor.liveStatus === 'LIVE'
                  : anchor.liveStatus === 'OFFLINE').length }}
            </small>
          </button>
        </div>
      </div>

      <div class="user-home__rule" aria-hidden="true"></div>

      <section class="user-home__content" aria-labelledby="user-home-title" :aria-busy="isLoading">
        <div v-if="status === 'idle' || isLoading" class="user-home__state">
          <div class="user-home__state-mark" aria-hidden="true">◌</div>
          <p>正在加载关注主播…</p>
        </div>

        <div v-else-if="status === 'error'" class="user-home__state user-home__state--error">
          <div class="user-home__state-mark" aria-hidden="true">!</div>
          <h2>关注列表暂时无法加载</h2>
          <p>{{ errorMessage || '请稍后重试' }}</p>
          <button class="user-home__state-action" type="button" @click="loadHome">重新加载</button>
        </div>

        <div v-else-if="anchors.length === 0" class="user-home__state">
          <CornerOrnaments color="var(--color-gold-border)" :size="14" />
          <div class="user-home__state-mark" aria-hidden="true">✦</div>
          <h2>暂未关注任何主播</h2>
          <p>粘贴直播间链接，开始建立你的跨平台直播雷达</p>
          <button class="user-home__state-action" type="button" @click="openAddModal">添加第一位主播</button>
        </div>

        <div v-else-if="filteredCards.length === 0" class="user-home__state">
          <div class="user-home__state-mark" aria-hidden="true">⌁</div>
          <h2>当前筛选没有结果</h2>
          <p>清除平台或状态筛选，查看全部关注主播</p>
          <button class="user-home__state-action" type="button" @click="clearFilters">清除筛选</button>
        </div>

        <div v-else class="user-home__card-grid">
          <div v-for="(card, index) in filteredCards" :key="card.id" class="user-home__card-column">
            <StreamerCard
              :streamer="card"
              :delay="index * 0.05"
              :removable="true"
              :batch-mode="batchMode"
              :selected="selectedFollowIds.has(Number(card.id))"
              @remove="requestUnfollow(card)"
              @toggle-select="toggleSelected(Number(card.id))"
            />
          </div>
        </div>
      </section>
    </div>

    <AddStreamerModal
      v-if="showAddModal"
      :submitting="isSubmitting"
      :error-message="addErrorMessage"
      @close="showAddModal = false"
      @submit="handleFollow"
    />

    <ConfirmModal
      v-if="pendingUnfollow"
      title="取消关注"
      :message="`确认取消关注「${pendingUnfollow.name}」？`"
      confirm-label="确认取关"
      danger
      @close="pendingUnfollow = null"
      @confirm="confirmUnfollow"
    />

    <ConfirmModal
      v-if="pendingBatchUnfollow"
      title="批量取消关注"
      :message="`确认取消关注选中的 ${selectedCount} 位主播？`"
      confirm-label="确认取关"
      danger
      @close="pendingBatchUnfollow = false"
      @confirm="confirmBatchUnfollow"
    />
  </main>
</template>
