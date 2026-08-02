<script setup lang="ts">
interface Props {
  /** 页面上方的小标题。 */
  eyebrow: string
  /** 页面主标题。 */
  title: string
  /** 当前阶段说明。 */
  description: string
  /** 主操作按钮文案。 */
  actionLabel?: string
  /** 主操作按钮地址。 */
  actionTo?: string
  /** 次操作按钮文案。 */
  secondaryLabel?: string
  /** 次操作按钮地址。 */
  secondaryTo?: string
}

withDefaults(defineProps<Props>(), {
  actionLabel: '回到观测台',
  actionTo: '/',
  secondaryLabel: '浏览登录入口',
  secondaryTo: '/login',
})
</script>

<template>
  <section class="placeholder-page page-container" aria-labelledby="placeholder-title">
    <div class="placeholder-page__content">
      <p class="eyebrow">{{ eyebrow }}</p>
      <h1 id="placeholder-title">{{ title }}</h1>
      <p class="placeholder-page__description">{{ description }}</p>

      <div class="placeholder-page__status" aria-label="页面状态">
        <span class="status-pill status-pill--ready">基础页面已就绪</span>
        <span class="status-pill">功能接入阶段</span>
      </div>

      <div class="placeholder-page__actions">
        <RouterLink class="gold-button" :to="actionTo">{{ actionLabel }}</RouterLink>
        <RouterLink class="outline-button" :to="secondaryTo">{{ secondaryLabel }}</RouterLink>
      </div>
    </div>

    <aside class="placeholder-page__aside" aria-label="当前开发范围">
      <span class="placeholder-page__aside-mark" aria-hidden="true">◎</span>
      <p class="eyebrow">CURRENT SCOPE</p>
      <p>当前仅提供可访问的页面骨架。账号会话、关注关系与实时数据将在后续阶段接入。</p>
    </aside>
  </section>
</template>
