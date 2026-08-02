<script setup lang="ts">
interface Props {
  /** 分隔线整体透明度。 */
  opacity?: number
}

withDefaults(defineProps<Props>(), {
  opacity: 1,
})
</script>

<template>
  <div class="gold-divider" :style="{ opacity }" aria-hidden="true">
    <span class="gold-divider__line gold-divider__line--left"></span>
    <span class="gold-divider__mark">✦</span>
    <span class="gold-divider__line gold-divider__line--right"></span>
  </div>
</template>
