<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  /** 角标线条颜色。 */
  color?: string
  /** 角标边长。 */
  size?: number
}

const props = withDefaults(defineProps<Props>(), {
  color: 'rgba(200, 169, 110, 0.35)',
  size: 12,
})

const corners = computed(() => {
  const { size } = props
  const turn = Math.round(size * 0.67)

  return [
    { key: 'top-left', style: { top: 0, left: 0 }, path: `M 0 ${turn} L 0 0 L ${turn} 0` },
    { key: 'top-right', style: { top: 0, right: 0 }, path: `M ${size} ${turn} L ${size} 0 L ${size - turn} 0` },
    { key: 'bottom-right', style: { right: 0, bottom: 0 }, path: `M ${size} ${size - turn} L ${size} ${size} L ${size - turn} ${size}` },
    { key: 'bottom-left', style: { bottom: 0, left: 0 }, path: `M 0 ${size - turn} L 0 ${size} L ${turn} ${size}` },
  ]
})
</script>

<template>
  <svg
    v-for="corner in corners"
    :key="corner.key"
    class="corner-ornament"
    :width="size"
    :height="size"
    :viewBox="`0 0 ${size} ${size}`"
    fill="none"
    aria-hidden="true"
    :style="corner.style"
  >
    <path :d="corner.path" :stroke="color" stroke-width="1.2" />
  </svg>
</template>
