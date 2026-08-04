<script setup lang="ts">
import { computed, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute, useRouter } from 'vue-router'

import AppToast from '@/components/AppToast.vue'
import { useSessionStore } from '@/stores/session'
import { useToastStore } from '@/stores/toast'

const router = useRouter()
const route = useRoute()
const sessionStore = useSessionStore()
const toastStore = useToastStore()
const { user } = storeToRefs(sessionStore)
const menuOpen = ref(false)
const isLoggingOut = ref(false)
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
 * 切换顶部用户菜单。
 */
function toggleMenu() {
  menuOpen.value = !menuOpen.value
}

/**
 * 关闭顶部用户菜单。
 */
function closeMenu() {
  menuOpen.value = false
}

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

      <div v-if="user" class="header-account">
        <button
          class="header-user"
          :class="{ 'header-user--admin': isAdmin }"
          type="button"
          aria-haspopup="menu"
          :aria-expanded="menuOpen"
          @click="toggleMenu"
        >
          <span class="header-user__avatar">
            <img v-if="headerAvatarPath" :src="headerAvatarPath" :alt="headerAvatarAlt" />
            <span v-else aria-hidden="true">{{ user.nickname.slice(0, 1) }}</span>
          </span>
          <span>{{ user.nickname }}</span>
          <span class="header-user__arrow" aria-hidden="true">▾</span>
        </button>

        <div v-if="menuOpen" class="header-menu" role="menu">
          <RouterLink class="header-menu__link" to="/user-center" role="menuitem" @click="closeMenu">
            用户中心
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
