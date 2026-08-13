<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute, useRouter } from 'vue-router'

import AppToast from '@/components/AppToast.vue'
import { useSessionStore } from '@/stores/session'
import { useLiveStore } from '@/stores/live'
import { useToastStore } from '@/stores/toast'

const router = useRouter()
const route = useRoute()
const sessionStore = useSessionStore()
const liveStore = useLiveStore()
const toastStore = useToastStore()
const { user } = storeToRefs(sessionStore)
const { status: liveHomeStatus, totalCount: followedAnchorCount } = storeToRefs(liveStore)
const menuOpen = ref(false)
const isLoggingOut = ref(false)
let menuCloseTimer: ReturnType<typeof setTimeout> | null = null
const ADMIN_AVATAR_PATH = '/avatars/avatar-01.svg'
const isAdmin = computed(() => user.value?.role === 'ADMIN')
const headerAvatarPath = computed(() => (
  user.value?.avatarPath || (isAdmin.value ? ADMIN_AVATAR_PATH : null)
))
const headerAvatarAlt = computed(() => (
  isAdmin.value && !user.value?.avatarPath
    ? '管理员默认头像'
    : `${user.value?.nickname || '用户'}的头像`
))
const shouldHideScrollbar = computed(() => (
  route.name === 'home' || route.name === 'login' || route.name === 'register'
))

/**
 * 清理用户菜单延时关闭定时器。
 */
function clearMenuCloseTimer() {
  if (menuCloseTimer) {
    clearTimeout(menuCloseTimer)
    menuCloseTimer = null
  }
}

/**
 * 打开顶部用户菜单。
 */
function openMenu() {
  clearMenuCloseTimer()
  menuOpen.value = true
}

/**
 * 点击头像切换顶部用户菜单，兼容触摸设备。
 */
function toggleMenu() {
  if (menuOpen.value) {
    closeMenu()
    return
  }
  openMenu()
}

/**
 * 关闭顶部用户菜单。
 */
function closeMenu() {
  clearMenuCloseTimer()
  menuOpen.value = false
}

/**
 * 鼠标离开头像和菜单区域后延时关闭，避免经过菜单间隙时闪退。
 */
function scheduleMenuClose() {
  clearMenuCloseTimer()
  menuCloseTimer = setTimeout(() => {
    menuCloseTimer = null
    menuOpen.value = false
  }, 140)
}

/**
 * 键盘焦点离开整个用户菜单区域后关闭菜单。
 *
 * @param event 焦点离开事件
 */
function handleMenuFocusOut(event: FocusEvent) {
  const accountElement = event.currentTarget
  const nextTarget = event.relatedTarget
  if (accountElement instanceof HTMLElement && nextTarget instanceof Node && accountElement.contains(nextTarget)) {
    return
  }
  scheduleMenuClose()
}

watch(
  [user, () => route.name],
  ([currentUser, routeName]) => {
    if (!currentUser) {
      liveStore.clear()
      closeMenu()
      return
    }

    // 首页由 UserHomePage 负责加载，其他认证页面按需加载关注总数。
    if (routeName !== 'home' && liveHomeStatus.value === 'idle') {
      void liveStore.loadHome().catch(() => undefined)
    }
  },
  { immediate: true },
)

/**
 * 调用后端退出接口并返回首页。
 */
async function handleLogout() {
  if (isLoggingOut.value) {
    return
  }

  isLoggingOut.value = true
  try {
    await sessionStore.logout()
    closeMenu()
    await router.replace({ name: 'home' })
    toastStore.show('已退出登录', 'success')
  } catch {
    toastStore.show(sessionStore.errorMessage || '退出失败，请稍后重试', 'error')
  } finally {
    isLoggingOut.value = false
  }
}
</script>

<template>
  <div class="figma-app-shell" :class="{ 'figma-app-shell--no-scrollbar': shouldHideScrollbar }">
    <div class="top-gold-rule" aria-hidden="true"></div>
    <AppToast />

    <header class="figma-header">
      <RouterLink class="figma-logo" to="/" aria-label="StreamRadar 首页">
        Stream<span>Radar</span>
      </RouterLink>

      <div
        v-if="user"
        class="header-account"
        @mouseenter="openMenu"
        @mouseleave="scheduleMenuClose"
        @focusin="openMenu"
        @focusout="handleMenuFocusOut"
      >
        <button
          class="header-user"
          :class="{ 'header-user--admin': isAdmin }"
          type="button"
          aria-label="打开用户菜单"
          aria-haspopup="menu"
          :aria-expanded="menuOpen"
          @click="toggleMenu"
        >
          <span class="header-user__avatar">
            <img v-if="headerAvatarPath" :src="headerAvatarPath" :alt="headerAvatarAlt" />
            <span v-else aria-hidden="true">{{ user.nickname.slice(0, 1) }}</span>
          </span>
        </button>

        <div
          v-if="menuOpen"
          class="header-menu"
          role="menu"
          @mouseenter="openMenu"
          @mouseleave="scheduleMenuClose"
        >
          <div class="header-menu__profile" role="group" aria-label="用户信息">
            <span class="header-menu__profile-avatar">
              <img v-if="headerAvatarPath" :src="headerAvatarPath" :alt="headerAvatarAlt" />
              <span v-else aria-hidden="true">{{ user.nickname.slice(0, 1) }}</span>
            </span>
            <span class="header-menu__profile-copy">
              <strong>{{ user.nickname }}</strong>
              <span>关注 {{ followedAnchorCount }}</span>
            </span>
          </div>

          <RouterLink class="header-menu__link" to="/user-center" role="menuitem" @click="closeMenu">
            个人中心
          </RouterLink>
          <RouterLink
            v-if="isAdmin"
            class="header-menu__link header-menu__admin"
            to="/admin"
            role="menuitem"
            @click="closeMenu"
          >
            管理中心
          </RouterLink>
          <button
            class="header-menu__link header-menu__logout"
            type="button"
            role="menuitem"
            :disabled="isLoggingOut"
            @click="handleLogout"
          >
            {{ isLoggingOut ? '正在退出…' : '退出登录' }}
          </button>
        </div>
      </div>

    </header>

    <main class="app-content">
      <RouterView />
    </main>

    <footer class="figma-footer">
      <div class="figma-footer__content">
        <span class="figma-footer__mark" aria-hidden="true">✦</span>
        <span>STREAMRADAR · 跨平台直播状态监控</span>
        <span class="figma-footer__mark" aria-hidden="true">✦</span>
      </div>
    </footer>
  </div>
</template>
