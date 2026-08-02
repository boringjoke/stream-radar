<script setup lang="ts">
import { ref } from 'vue'

import CornerOrnaments from '@/components/CornerOrnaments.vue'
import GoldDivider from '@/components/GoldDivider.vue'

interface Props {
  /** 当前账号页面模式。 */
  mode: 'login' | 'register'
}

const props = defineProps<Props>()
const identifier = ref('')
const password = ref('')
const nickname = ref('')
const notice = ref('')

function handleSubmit() {
  notice.value = props.mode === 'login'
    ? '登录接口将在下一阶段接入，当前不会提交账号信息。'
    : '注册接口将在下一阶段接入，当前不会提交账号信息。'
}
</script>

<template>
  <main class="account-page">
    <div class="account-panel animate-fade-in-up">
      <CornerOrnaments color="var(--color-gold)" :size="14" />

      <div class="account-panel__heading">
        <GoldDivider />
        <div class="account-panel__kicker">{{ mode === 'login' ? 'SIGN IN' : 'SIGN UP' }}</div>
        <h1>Stream<span>Radar</span></h1>
        <p>{{ mode === 'login' ? '登录以管理您关注的主播' : '建立你的跨平台直播雷达' }}</p>
      </div>

      <form class="account-form" @submit.prevent="handleSubmit">
        <label v-if="mode === 'register'" class="account-field">
          <span>NICKNAME</span>
          <input v-model="nickname" type="text" placeholder="你的昵称" />
        </label>

        <label class="account-field">
          <span>{{ mode === 'login' ? 'EMAIL' : 'EMAIL' }}</span>
          <input v-model="identifier" type="email" placeholder="your@email.com" />
        </label>

        <label class="account-field">
          <span>PASSWORD</span>
          <input v-model="password" type="password" placeholder="••••••••" />
        </label>

        <p v-if="notice" class="account-notice" role="status">{{ notice }}</p>

        <button class="account-submit" type="submit">
          {{ mode === 'login' ? '登　录' : '注　册' }}
        </button>
      </form>

      <div class="account-panel__footer">
        <p>当前仅展示页面，不提交账号信息</p>
        <RouterLink v-if="mode === 'login'" to="/register">还没有账号？注册</RouterLink>
        <RouterLink v-else to="/login">已有账号？登录</RouterLink>
        <RouterLink class="account-panel__back" to="/">← 返回首页</RouterLink>
      </div>
    </div>
  </main>
</template>
