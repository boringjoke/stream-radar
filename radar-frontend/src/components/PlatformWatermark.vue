<script setup lang="ts">
interface Props {
  /** 平台编码。 */
  platform: 'bilibili' | 'douyu' | 'huya' | 'douyin'
}

defineProps<Props>()
</script>

<template>
  <svg
    class="platform-watermark"
    viewBox="0 0 100 100"
    fill="none"
    aria-hidden="true"
  >
    <template v-if="platform === 'bilibili'">
      <rect x="10" y="22" width="80" height="58" rx="10" fill="currentColor" />
      <circle cx="33" cy="16" r="7" fill="currentColor" />
      <circle cx="67" cy="16" r="7" fill="currentColor" />
      <rect x="26" y="44" width="11" height="18" rx="3" fill="var(--color-bg)" />
      <rect x="63" y="44" width="11" height="18" rx="3" fill="var(--color-bg)" />
    </template>
    <template v-else-if="platform === 'douyu'">
      <path d="M12 18 L88 18 L88 66 L56 66 L44 82 L44 66 L12 66 Z" fill="currentColor" />
      <rect x="28" y="32" width="9" height="20" rx="2.5" fill="var(--color-bg)" />
      <rect x="45" y="32" width="9" height="20" rx="2.5" fill="var(--color-bg)" />
      <rect x="62" y="32" width="9" height="20" rx="2.5" fill="var(--color-bg)" />
    </template>
    <template v-else-if="platform === 'huya'">
      <path d="M50 10 C24 10 10 27 10 50 C10 73 24 90 50 90 C76 90 90 73 90 50 C90 27 76 10 50 10Z" fill="currentColor" />
      <path d="M35 33 L35 67 L50 53 L65 67 L65 33" stroke="var(--color-bg)" stroke-width="8" stroke-linecap="round" stroke-linejoin="round" />
    </template>
    <template v-else>
      <path d="M57 13 C57 13 61 29 78 32 L78 46 C78 46 63 44 57 34 L57 67 C57 81 46 90 35 86 C24 82 19 70 25 60 C31 50 45 50 51 56 L51 70 C49 68 45 67 42 69 C39 71 39 76 42 78 C45 80 51 78 51 72 L51 13 Z" fill="currentColor" />
    </template>
  </svg>
</template>
