<script setup lang="ts">
import { storeToRefs } from 'pinia'

import { useToastStore } from '@/stores/toast'

const toastStore = useToastStore()
const { message, type, visible } = storeToRefs(toastStore)
</script>

<template>
  <Transition name="toast">
    <div
      v-if="visible"
      class="app-toast"
      :class="`app-toast--${type}`"
      :role="type === 'error' ? 'alert' : 'status'"
      aria-atomic="true"
      :aria-live="type === 'error' ? 'assertive' : 'polite'"
    >
      <span class="app-toast__icon" aria-hidden="true">
        {{ type === 'success' ? '✓' : type === 'error' ? '!' : 'i' }}
      </span>
      <p class="app-toast__message">{{ message }}</p>
      <button class="app-toast__close" type="button" aria-label="关闭提示" @click="toastStore.hide">
        ×
      </button>
    </div>
  </Transition>
</template>
