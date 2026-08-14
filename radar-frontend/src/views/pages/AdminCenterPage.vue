<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { ApiRequestError } from '@/api/request'
import { getAdminLiveAnchorPage, getAdminOverview } from '@/api/admin'
import { useSessionStore } from '@/stores/session'
import { useToastStore } from '@/stores/toast'
import { platformCatalog } from '@/types/platform'
import type { LivePlatformCode } from '@/types/live'
import type {
  AdminLiveAnchor,
  AdminLiveAnchorPage,
  AdminLiveAnchorPageRequest,
  AdminOverview,
} from '@/types/admin'

type PlatformKey = keyof typeof platformCatalog

interface AdminFilterState {
  platform: LivePlatformCode | ''
  anchorName: string
  roomId: string
  minFollowerCount: string
  maxFollowerCount: string
}

const PLATFORM_MAP: Record<LivePlatformCode, PlatformKey> = {
  BILIBILI: 'bilibili',
  DOUYU: 'douyu',
  HUYA: 'huya',
  DOUYIN: 'douyin',
}

const platformCodes: LivePlatformCode[] = ['BILIBILI', 'DOUYU', 'HUYA', 'DOUYIN']
const platformOptions: Array<{ value: LivePlatformCode | ''; label: string }> = [
  { value: '', label: '全部平台' },
  ...platformCodes.map((code) => ({
    value: code,
    label: platformCatalog[PLATFORM_MAP[code]].label,
  })),
]

const route = useRoute()
const router = useRouter()
const sessionStore = useSessionStore()
const toastStore = useToastStore()

const overview = ref<AdminOverview | null>(null)
const page = ref<AdminLiveAnchorPage>({
  pageNum: 1,
  pageSize: 20,
  total: 0,
  records: [],
})
const filters = ref<AdminFilterState>({
  platform: '',
  anchorName: '',
  roomId: '',
  minFollowerCount: '',
  maxFollowerCount: '',
})
const isOverviewLoading = ref(true)
const isPageLoading = ref(true)
const overviewError = ref('')
const pageError = ref('')
const filterError = ref('')

const totalPages = computed(() => Math.max(1, Math.ceil(page.value.total / page.value.pageSize)))
const activePageRange = computed(() => {
  if (page.value.total === 0) {
    return '0 / 0 条记录'
  }
  const start = (page.value.pageNum - 1) * page.value.pageSize + 1
  const end = Math.min(page.value.pageNum * page.value.pageSize, page.value.total)
  return `${start}—${end} / ${page.value.total} 条记录`
})

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
 * 处理管理接口的认证和权限失效。
 *
 * @param error 请求异常
 * @returns 是否已经完成跳转
 */
async function redirectForAccessError(error: unknown): Promise<boolean> {
  if (!(error instanceof ApiRequestError)) {
    return false
  }
  if (error.status === 401) {
    sessionStore.clear()
    await router.replace({
      name: 'login',
      query: { redirect: route.fullPath },
    })
    return true
  }
  if (error.status === 403) {
    toastStore.show('权限不足，仅管理员可访问管理中心', 'error')
    await router.replace({ name: 'home' })
    return true
  }
  return false
}

/**
 * 加载管理中心统计。
 */
async function loadOverview() {
  isOverviewLoading.value = true
  overviewError.value = ''
  try {
    overview.value = await getAdminOverview()
  } catch (error) {
    if (await redirectForAccessError(error)) {
      return
    }
    overviewError.value = getErrorMessage(error)
  } finally {
    isOverviewLoading.value = false
  }
}

/**
 * 将输入框中的关注人数转换为后端查询参数。
 *
 * @param value 输入值
 * @param label 字段名称
 * @returns 非负整数或空值
 */
function parseFollowerCount(value: string, label: string): number | null {
  const normalized = value.trim()
  if (!normalized) {
    return null
  }
  if (!/^\d+$/.test(normalized)) {
    throw new Error(`${label}请输入非负整数`)
  }
  const count = Number(normalized)
  if (!Number.isSafeInteger(count)) {
    throw new Error(`${label}数值过大`)
  }
  return count
}

/**
 * 生成当前筛选条件对应的分页请求。
 *
 * @returns 分页请求，输入不合法时返回 null
 */
function buildPageRequest(): AdminLiveAnchorPageRequest | null {
  try {
    const minFollowerCount = parseFollowerCount(filters.value.minFollowerCount, '最小关注人数')
    const maxFollowerCount = parseFollowerCount(filters.value.maxFollowerCount, '最大关注人数')
    if (minFollowerCount !== null && maxFollowerCount !== null && minFollowerCount > maxFollowerCount) {
      throw new Error('最小关注人数不能大于最大关注人数')
    }
    return {
      pageNum: page.value.pageNum,
      pageSize: page.value.pageSize,
      platform: filters.value.platform,
      anchorName: filters.value.anchorName,
      roomId: filters.value.roomId,
      minFollowerCount,
      maxFollowerCount,
    }
  } catch (error) {
    filterError.value = error instanceof Error ? error.message : '关注人数筛选条件无效'
    return null
  }
}

/**
 * 加载主播目录。
 */
async function loadPage() {
  const request = buildPageRequest()
  if (!request) {
    return
  }
  isPageLoading.value = true
  pageError.value = ''
  try {
    page.value = await getAdminLiveAnchorPage(request)
  } catch (error) {
    if (await redirectForAccessError(error)) {
      return
    }
    pageError.value = getErrorMessage(error)
  } finally {
    isPageLoading.value = false
  }
}

/**
 * 提交筛选条件并回到第一页。
 */
function applyFilters() {
  filterError.value = ''
  page.value.pageNum = 1
  void loadPage()
}

/**
 * 清空筛选条件并重新查询。
 */
function resetFilters() {
  filters.value = {
    platform: '',
    anchorName: '',
    roomId: '',
    minFollowerCount: '',
    maxFollowerCount: '',
  }
  filterError.value = ''
  page.value.pageNum = 1
  void loadPage()
}

/**
 * 切换主播目录页码。
 *
 * @param step 页码变化量
 */
function changePage(step: number) {
  const nextPage = page.value.pageNum + step
  if (nextPage < 1 || nextPage > totalPages.value || isPageLoading.value) {
    return
  }
  page.value.pageNum = nextPage
  void loadPage()
}

/**
 * 获取平台展示元数据。
 *
 * @param platform 后端平台标识
 * @returns 平台展示元数据
 */
function getPlatformMeta(platform: LivePlatformCode) {
  return platformCatalog[PLATFORM_MAP[platform]]
}

/**
 * 格式化数量，避免大数在目录中出现过长的无分隔文本。
 *
 * @param value 数量
 * @returns 展示文本
 */
function formatCount(value: number | null | undefined): string {
  return new Intl.NumberFormat('zh-CN').format(value ?? 0)
}

/**
 * 获取主播头像缺省字母。
 *
 * @param anchor 主播记录
 * @returns 缺省头像文字
 */
function getAvatarFallback(anchor: AdminLiveAnchor): string {
  return anchor.anchorName?.trim().slice(0, 1) || '播'
}

onMounted(() => {
  void Promise.all([loadOverview(), loadPage()])
})
</script>

<template>
  <main class="admin-center-page" aria-labelledby="admin-center-title">
    <div class="admin-center-page__inner">
      <header class="admin-center-heading">
        <div>
          <span class="admin-center-heading__kicker">ADMIN CONSOLE · READ ONLY</span>
          <h1 id="admin-center-title">平台观测总览</h1>
          <p>查看用户、主播与关注关系的当前快照，目录包含所有已登记主播。</p>
        </div>
        <span class="admin-center-heading__status"><i aria-hidden="true"></i>只读视图</span>
      </header>

      <section class="admin-overview" aria-label="全平台统计" :aria-busy="isOverviewLoading">
        <div v-if="isOverviewLoading" class="admin-center-state admin-center-state--loading">
          正在读取平台快照…
        </div>
        <div v-else-if="overviewError" class="admin-center-state admin-center-state--error" role="alert">
          <span>{{ overviewError }}</span>
          <button type="button" @click="loadOverview">重新加载统计</button>
        </div>
        <template v-else-if="overview">
          <div class="admin-stat-grid">
            <article class="admin-stat-card admin-stat-card--primary">
              <span class="admin-stat-card__label">ACTIVE USERS</span>
              <strong>{{ formatCount(overview.userCount) }}</strong>
              <span class="admin-stat-card__caption">启用普通用户</span>
            </article>
            <article class="admin-stat-card">
              <span class="admin-stat-card__label">ALL ANCHORS</span>
              <strong>{{ formatCount(overview.anchorCount) }}</strong>
              <span class="admin-stat-card__caption">纳入统计的主播</span>
            </article>
            <article class="admin-stat-card">
              <span class="admin-stat-card__label">FOLLOWED ANCHORS</span>
              <strong>{{ formatCount(overview.followedAnchorCount) }}</strong>
              <span class="admin-stat-card__caption">被启用普通用户关注</span>
            </article>
          </div>

          <div class="admin-platform-grid" aria-label="平台主播数量">
            <article
              v-for="platformCode in platformCodes"
              :key="platformCode"
              class="admin-platform-stat"
              :style="{ '--platform-color': getPlatformMeta(platformCode).color }"
            >
              <div class="admin-platform-stat__topline">
                <span class="admin-platform-stat__icon">
                  <img :src="getPlatformMeta(platformCode).iconPath" :alt="`${getPlatformMeta(platformCode).label}图标`" />
                </span>
                <span>{{ getPlatformMeta(platformCode).roman }}</span>
              </div>
              <strong>{{ formatCount(overview.platformAnchorCounts[platformCode]) }}</strong>
              <span>{{ getPlatformMeta(platformCode).label }}主播</span>
            </article>
          </div>
        </template>
      </section>

      <section class="admin-anchor-directory" aria-label="主播数据" :aria-busy="isPageLoading">
        <div class="admin-directory-note">
          <p>按主播昵称首字母升序排列；关注人数只计算启用普通用户的去重关注。</p>
        </div>

        <form class="admin-filter-bar" @submit.prevent="applyFilters">
          <label class="admin-filter-field admin-filter-field--platform">
            <span>平台</span>
            <select v-model="filters.platform">
              <option v-for="option in platformOptions" :key="option.value || 'all'" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </label>

          <label class="admin-filter-field">
            <span>主播名</span>
            <input v-model="filters.anchorName" type="search" placeholder="模糊包含查询" />
          </label>

          <label class="admin-filter-field">
            <span>房间号</span>
            <input v-model="filters.roomId" type="text" inputmode="numeric" placeholder="精确匹配" />
          </label>

          <label class="admin-filter-field admin-filter-field--count">
            <span>最小关注人数</span>
            <input v-model="filters.minFollowerCount" type="text" inputmode="numeric" placeholder="不限" />
          </label>

          <label class="admin-filter-field admin-filter-field--count">
            <span>最大关注人数</span>
            <input v-model="filters.maxFollowerCount" type="text" inputmode="numeric" placeholder="不限" />
          </label>

          <div class="admin-filter-actions">
            <button class="admin-filter-button admin-filter-button--primary" type="submit">查询目录</button>
            <button class="admin-filter-button" type="button" @click="resetFilters">重置</button>
          </div>
        </form>
        <p v-if="filterError" class="admin-filter-error" role="alert">{{ filterError }}</p>

        <div class="admin-directory-meta">
          <span>{{ activePageRange }}</span>
          <span>ALL ANCHORS · A—Z</span>
        </div>

        <div v-if="isPageLoading" class="admin-center-state admin-center-state--loading">
          正在读取主播目录…
        </div>
        <div v-else-if="pageError" class="admin-center-state admin-center-state--error" role="alert">
          <span>{{ pageError }}</span>
          <button type="button" @click="loadPage">重新加载目录</button>
        </div>
        <div v-else-if="page.records.length === 0" class="admin-center-state admin-center-state--empty">
          当前筛选条件下没有主播记录。
        </div>
        <div v-else class="admin-table-wrap">
          <table class="admin-anchor-table">
            <thead>
              <tr>
                <th scope="col">平台</th>
                <th scope="col">主播</th>
                <th scope="col">直播间地址</th>
                <th scope="col" class="admin-anchor-table__count-heading">关注人数</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="anchor in page.records" :key="anchor.anchorId">
                <td>
                  <span
                    class="admin-platform-chip"
                    :style="{ '--platform-color': getPlatformMeta(anchor.platform).color }"
                  >
                    <img :src="getPlatformMeta(anchor.platform).iconPath" :alt="`${getPlatformMeta(anchor.platform).label}图标`" />
                    <span>{{ getPlatformMeta(anchor.platform).label }}</span>
                  </span>
                </td>
                <td>
                  <div class="admin-anchor-cell">
                    <span class="admin-anchor-avatar">
                      <img
                        v-if="anchor.avatarUrl"
                        :src="anchor.avatarUrl"
                        :alt="`${anchor.anchorName || anchor.roomId}头像`"
                      />
                      <span v-else aria-hidden="true">{{ getAvatarFallback(anchor) }}</span>
                    </span>
                    <span class="admin-anchor-cell__copy">
                      <strong>{{ anchor.anchorName?.trim() || `房间 ${anchor.roomId}` }}</strong>
                      <small>ROOM {{ anchor.roomId }}</small>
                    </span>
                  </div>
                </td>
                <td>
                  <a class="admin-anchor-room-link" :href="anchor.roomUrl" target="_blank" rel="noopener noreferrer">
                    {{ anchor.roomUrl }}
                  </a>
                </td>
                <td class="admin-anchor-table__count-cell">
                  <strong>{{ formatCount(anchor.followerCount) }}</strong>
                  <small>启用普通用户</small>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <nav v-if="!isPageLoading && !pageError && page.total > 0" class="admin-pagination" aria-label="主播目录分页">
          <button type="button" :disabled="page.pageNum <= 1" @click="changePage(-1)">上一页</button>
          <span>PAGE {{ page.pageNum }} / {{ totalPages }}</span>
          <button type="button" :disabled="page.pageNum >= totalPages" @click="changePage(1)">下一页</button>
        </nav>
      </section>
    </div>
  </main>
</template>
