<script setup lang="ts">
import { computed } from 'vue'
import { storeToRefs } from 'pinia'

import CornerOrnaments from '@/components/CornerOrnaments.vue'
import SectionHeader from '@/components/SectionHeader.vue'
import { useSessionStore } from '@/stores/session'

const sessionStore = useSessionStore()
const { user } = storeToRefs(sessionStore)
const displayName = computed(() => user.value?.nickname || '未登录用户')
</script>

<template>
  <main class="user-center-page">
    <div class="user-center-page__inner">
      <RouterLink class="back-link" to="/">← 返回首页</RouterLink>

      <SectionHeader>用户中心</SectionHeader>

      <section class="profile-card" aria-labelledby="profile-title">
        <CornerOrnaments color="var(--color-gold-border)" :size="12" />

        <div class="profile-card__layout">
          <div class="profile-card__avatar" aria-hidden="true">
            <span>{{ displayName.slice(0, 1) }}</span>
          </div>

          <div class="profile-card__body">
            <template v-if="user">
              <h1 id="profile-title">{{ user.nickname }}</h1>
              <p class="profile-card__email">{{ user.username }}</p>
              <div class="profile-card__stats">
                <div>
                  <strong>—</strong>
                  <span>关注主播 <b aria-hidden="true">→</b></span>
                </div>
                <div>
                  <strong class="profile-card__stats-live">—</strong>
                  <span>正在直播 <b aria-hidden="true">→</b></span>
                </div>
              </div>
              <button class="profile-card__action" type="button" disabled>编辑资料</button>
            </template>

            <template v-else>
              <h1 id="profile-title">欢迎来到 Stream<span>Radar</span></h1>
              <p class="profile-card__email">登录后管理你的关注主播</p>
              <div class="profile-card__stats profile-card__stats--empty">
                <span>当前没有已登录的 Session</span>
              </div>
              <RouterLink class="profile-card__action profile-card__action--link" to="/login">登录开始使用</RouterLink>
            </template>
          </div>
        </div>
      </section>
    </div>
  </main>
</template>
