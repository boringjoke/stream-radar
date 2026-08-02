<script setup lang="ts">
import { storeToRefs } from 'pinia'

import { useSessionStore } from '@/stores/session'

const sessionStore = useSessionStore()
const { user } = storeToRefs(sessionStore)
</script>

<template>
  <div class="figma-app-shell">
    <div class="top-gold-rule" aria-hidden="true"></div>

    <header class="figma-header">
      <RouterLink class="figma-logo" to="/" aria-label="StreamRadar 首页">
        Stream<span>Radar</span>
      </RouterLink>

      <RouterLink v-if="user" class="header-user" to="/user-center">
        <span class="header-user__avatar" aria-hidden="true">
          {{ user.nickname.slice(0, 1) }}
        </span>
        <span>{{ user.nickname }}</span>
        <span class="header-user__arrow" aria-hidden="true">▾</span>
      </RouterLink>
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
