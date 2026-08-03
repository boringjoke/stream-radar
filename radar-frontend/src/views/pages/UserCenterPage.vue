<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute, useRouter } from 'vue-router'

import { ApiRequestError } from '@/api/request'
import { getAvatarOptions, getProfile, updateProfile } from '@/api/user'
import CornerOrnaments from '@/components/CornerOrnaments.vue'
import SectionHeader from '@/components/SectionHeader.vue'
import { useSessionStore } from '@/stores/session'
import { useToastStore } from '@/stores/toast'
import type { AvatarOption, UserProfile } from '@/types/user'

const route = useRoute()
const router = useRouter()
const sessionStore = useSessionStore()
const toastStore = useToastStore()
const { user } = storeToRefs(sessionStore)

const profile = ref<UserProfile | null>(null)
const avatarOptions = ref<AvatarOption[]>([])
const nickname = ref('')
const email = ref('')
const avatarPath = ref<string | null>(null)
const isLoading = ref(true)
const isEditing = ref(false)
const isSaving = ref(false)

const displayName = computed(() => profile.value?.nickname || user.value?.nickname || '未登录用户')
const displayAvatarPath = computed(() => profile.value?.avatarPath || user.value?.avatarPath)

/**
 * 将请求错误转换为用户中心提示。
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
 * 将资料响应同步到编辑表单。
 *
 * @param nextProfile 当前用户资料
 */
function syncForm(nextProfile: UserProfile) {
  nickname.value = nextProfile.nickname
  email.value = nextProfile.email || ''
  avatarPath.value = nextProfile.avatarPath
}

/**
 * 将资料响应同步到顶部 Session 用户摘要。
 *
 * @param nextProfile 当前用户资料
 */
function syncSessionUser(nextProfile: UserProfile) {
  sessionStore.setUser({
    id: nextProfile.id,
    username: nextProfile.username,
    nickname: nextProfile.nickname,
    avatarPath: nextProfile.avatarPath,
  })
}

/**
 * 查询用户资料和项目内置头像选项。
 */
async function loadProfile() {
  isLoading.value = true
  try {
    const [nextProfile, nextAvatarOptions] = await Promise.all([
      getProfile(),
      getAvatarOptions(),
    ])
    profile.value = nextProfile
    avatarOptions.value = nextAvatarOptions
    syncForm(nextProfile)
    syncSessionUser(nextProfile)
  } catch (error) {
    if (error instanceof ApiRequestError && error.status === 401) {
      sessionStore.clear()
      await router.replace({
        name: 'login',
        query: { redirect: route.fullPath },
      })
      return
    }
    toastStore.show(getErrorMessage(error), 'error')
  } finally {
    isLoading.value = false
  }
}

/**
 * 切换资料编辑状态。
 */
function toggleEditing() {
  if (isEditing.value && profile.value) {
    syncForm(profile.value)
  }
  isEditing.value = !isEditing.value
}

/**
 * 选择一个项目内置头像。
 *
 * @param path 头像静态资源路径，null 表示不使用头像
 */
function selectAvatar(path: string | null) {
  avatarPath.value = path
}

/**
 * 保存用户资料和头像选择。
 */
async function saveProfile() {
  if (isSaving.value || !profile.value) {
    return
  }

  const normalizedNickname = nickname.value.trim()
  const normalizedEmail = email.value.trim()

  if (!normalizedNickname) {
    toastStore.show('昵称不能为空', 'error')
    return
  }
  if (normalizedNickname.length > 64) {
    toastStore.show('昵称长度不能超过 64 位', 'error')
    return
  }

  isSaving.value = true
  try {
    const nextProfile = await updateProfile({
      nickname: normalizedNickname,
      email: normalizedEmail || null,
      avatarPath: avatarPath.value,
    })
    profile.value = nextProfile
    syncForm(nextProfile)
    syncSessionUser(nextProfile)
    isEditing.value = false
    toastStore.show('资料已保存', 'success')
  } catch (error) {
    if (error instanceof ApiRequestError && error.status === 401) {
      sessionStore.clear()
      await router.replace({
        name: 'login',
        query: { redirect: route.fullPath },
      })
      return
    }
    toastStore.show(getErrorMessage(error), 'error')
  } finally {
    isSaving.value = false
  }
}

onMounted(loadProfile)
</script>

<template>
  <main class="user-center-page">
    <div class="user-center-page__inner">
      <RouterLink class="back-link" to="/">← 返回首页</RouterLink>

      <SectionHeader>用户中心</SectionHeader>

      <section class="profile-card" aria-labelledby="profile-title" :aria-busy="isLoading">
        <CornerOrnaments color="var(--color-gold-border)" :size="12" />

        <div class="profile-card__layout">
          <div class="profile-card__avatar">
            <img v-if="displayAvatarPath" :src="displayAvatarPath" :alt="`${displayName}的头像`" />
            <span v-else aria-hidden="true">{{ displayName.slice(0, 1) }}</span>
          </div>

          <div class="profile-card__body">
            <template v-if="isLoading">
              <p class="profile-card__state">正在加载用户资料…</p>
            </template>

            <template v-else-if="profile">
              <h1 id="profile-title">{{ profile.nickname }}</h1>
              <p class="profile-card__email">{{ profile.email || `@${profile.username}` }}</p>
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

              <button class="profile-card__action profile-card__action--edit" type="button" @click="toggleEditing">
                {{ isEditing ? '取消编辑' : '编辑资料' }}
              </button>

              <form v-if="isEditing" class="user-profile-form" @submit.prevent="saveProfile">
                <label class="account-field">
                  <span>NICKNAME</span>
                  <input v-model="nickname" type="text" maxlength="64" autocomplete="nickname" required />
                </label>

                <label class="account-field">
                  <span>EMAIL <em>OPTIONAL</em></span>
                  <input v-model="email" type="email" autocomplete="email" />
                </label>

                <fieldset class="avatar-picker">
                  <legend>CHOOSE AVATAR</legend>
                  <div class="avatar-picker__options">
                    <button
                      class="avatar-option avatar-option--empty"
                      :class="{ 'avatar-option--selected': avatarPath === null }"
                      type="button"
                      :aria-pressed="avatarPath === null"
                      @click="selectAvatar(null)"
                    >
                      <span class="avatar-option__image" aria-hidden="true">—</span>
                      <span>无头像</span>
                    </button>
                    <button
                      v-for="option in avatarOptions"
                      :key="option.path"
                      class="avatar-option"
                      :class="{ 'avatar-option--selected': avatarPath === option.path }"
                      type="button"
                      :aria-label="`选择${option.name}`"
                      :aria-pressed="avatarPath === option.path"
                      @click="selectAvatar(option.path)"
                    >
                      <img class="avatar-option__image" :src="option.path" :alt="option.name" />
                      <span>{{ option.name }}</span>
                    </button>
                  </div>
                </fieldset>

                <button class="profile-card__save" type="submit" :disabled="isSaving">
                  {{ isSaving ? '保存中…' : '保存资料' }}
                </button>
              </form>

            </template>

            <template v-else>
              <h1 id="profile-title">暂时无法加载资料</h1>
              <p class="profile-card__email">资料暂时无法加载，请稍后重试</p>
              <button class="profile-card__action profile-card__action--edit" type="button" @click="loadProfile">
                重新加载
              </button>
            </template>
          </div>
        </div>
      </section>
    </div>
  </main>
</template>
