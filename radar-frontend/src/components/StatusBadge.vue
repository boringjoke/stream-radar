<script setup lang="ts">
type StreamStatus = 'live' | 'offline' | 'error'

interface Props {
  /** 主播当前状态。 */
  status: StreamStatus
}

defineProps<Props>()
</script>

<template>
  <span v-if="status === 'live'" class="status-badge status-badge--live">
    <i class="status-badge__live-dot" aria-hidden="true"></i>
    <span>LIVE</span>
  </span>
  <span v-else-if="status === 'error'" class="status-badge status-badge--error">
    <span aria-hidden="true">✦</span>
    <span>ERROR</span>
  </span>
  <span v-else class="status-badge status-badge--offline">OFFLINE</span>
</template>
