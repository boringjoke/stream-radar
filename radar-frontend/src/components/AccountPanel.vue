<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { ApiRequestError } from '@/api/request'
import CornerOrnaments from '@/components/CornerOrnaments.vue'
import GoldDivider from '@/components/GoldDivider.vue'
import { useSessionStore } from '@/stores/session'
import { useToastStore } from '@/stores/toast'

interface Props {
  /** 当前账号页面模式。 */
  mode: 'login' | 'register'
}

const props = defineProps<Props>()
const route = useRoute()
const router = useRouter()
const sessionStore = useSessionStore()
const toastStore = useToastStore()
const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const email = ref('')
const isSubmitting = ref(false)
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const passwordMismatch = computed(() => (
  props.mode === 'register'
  && password.value.length > 0
  && confirmPassword.value.length > 0
  && password.value !== confirmPassword.value
))
const emailInvalid = computed(() => {
  if (props.mode !== 'register') {
    return false
  }
  const normalizedEmail = email.value.trim()
  return normalizedEmail.length > 0
    && (normalizedEmail.length > 255 || !EMAIL_PATTERN.test(normalizedEmail))
})

/**
 * 取得登录成功后的安全跳转地址。
 *
 * @returns 当前站内跳转地址，非法或缺失时返回首页
 */
function getRedirectPath(): string {
  const redirect = route.query.redirect
  if (typeof redirect !== 'string' || !redirect.startsWith('/') || redirect.startsWith('//')) {
    return '/'
  }
  return redirect
}

/**
 * 将请求错误转换为账号页提示。
 *
 * @param error 请求错误
 * @returns 用户可读提示
 */
function getErrorMessage(error: unknown): string {
  if (error instanceof ApiRequestError) {
    return error.message
  }
  return '网络暂时不可用，请稍后重试'
}

/**
 * 校验表单并提交登录或注册请求。
 */
async function handleSubmit() {
  if (isSubmitting.value) {
    return
  }

  const normalizedUsername = username.value.trim()
  const normalizedEmail = email.value.trim()

  if (!normalizedUsername) {
    toastStore.show('请输入用户名', 'error')
    return
  }

  if (props.mode === 'register') {
    if (emailInvalid.value) {
      toastStore.show('请输入有效的邮箱格式', 'error')
      return
    }
    if (normalizedUsername.length < 4 || normalizedUsername.length > 32 || !/^[A-Za-z0-9_]+$/.test(normalizedUsername)) {
      toastStore.show('用户名需为 4～32 位英文字母、数字或下划线', 'error')
      return
    }
    if (password.value.length < 8 || password.value.length > 72) {
      toastStore.show('密码长度需为 8～72 位', 'error')
      return
    }
    if (!confirmPassword.value) {
      toastStore.show('请输入确认密码', 'error')
      return
    }
    if (passwordMismatch.value) {
      toastStore.show('两次输入的密码不一致', 'error')
      return
    }
  } else if (password.value.length === 0 || password.value.length > 72) {
    toastStore.show('请输入有效密码', 'error')
    return
  }

  isSubmitting.value = true
  try {
    if (props.mode === 'register') {
      await sessionStore.register({
        username: normalizedUsername,
        password: password.value,
        confirmPassword: confirmPassword.value,
        email: normalizedEmail || null,
      })
    } else {
      await sessionStore.login({
        username: normalizedUsername,
        password: password.value,
      })
    }
    toastStore.show(props.mode === 'login' ? '登录成功' : '注册成功', 'success')
    await router.replace(getRedirectPath())
  } catch (error) {
    toastStore.show(getErrorMessage(error), 'error')
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
    <main class="account-page account-page--auth">
    <div class="account-panel animate-fade-in-up">
      <CornerOrnaments color="var(--color-gold)" :size="14" />

      <div class="account-panel__heading">
        <GoldDivider />
        <div class="account-panel__kicker">{{ mode === 'login' ? 'SIGN IN' : 'SIGN UP' }}</div>
        <h1>Stream<span>Radar</span></h1>
        <p>{{ mode === 'login' ? '登录以管理您关注的主播' : '建立你的跨平台直播雷达' }}</p>
      </div>

      <form class="account-form" novalidate @submit.prevent="handleSubmit">
        <label class="account-field">
          <span>用户名</span>
          <input
            v-model="username"
            type="text"
            placeholder="你的用户名"
            autocomplete="username"
            required
          />
        </label>

        <label v-if="mode === 'register'" class="account-field">
          <span>邮箱(选填)</span>
          <input
            v-model="email"
            type="email"
            placeholder="your@email.com"
            autocomplete="email"
            :aria-invalid="emailInvalid"
            aria-describedby="email-format-message"
          />
        </label>

        <p v-if="emailInvalid" id="email-format-message" class="account-field__error" role="alert">
          请输入有效的邮箱格式
        </p>

        <label class="account-field">
          <span>密码</span>
          <input
            v-model="password"
            type="password"
            placeholder="请输入密码"
            :autocomplete="mode === 'login' ? 'current-password' : 'new-password'"
            required
          />
        </label>

        <label v-if="mode === 'register'" class="account-field">
          <span>确认密码</span>
          <input
            v-model="confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            autocomplete="new-password"
            :aria-invalid="passwordMismatch"
            aria-describedby="password-mismatch-message"
            required
          />
        </label>

        <p v-if="passwordMismatch" id="password-mismatch-message" class="account-field__error" role="alert">
          两次输入的密码不一致
        </p>

        <button class="account-submit" type="submit" :disabled="isSubmitting">
          {{ isSubmitting ? '处理中…' : (mode === 'login' ? '登　录' : '注　册') }}
        </button>
      </form>

      <div class="account-panel__footer">
        <p>登录状态由安全 Session 保护</p>
        <RouterLink v-if="mode === 'login'" to="/register">还没有账号？注册</RouterLink>
        <RouterLink v-else to="/login">已有账号？登录</RouterLink>
        <RouterLink class="account-panel__back" to="/">← 返回首页</RouterLink>
      </div>
    </div>
  </main>
</template>
