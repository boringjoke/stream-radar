<script setup lang="ts">
import CornerOrnaments from '@/components/CornerOrnaments.vue'
import SectionHeader from '@/components/SectionHeader.vue'
</script>

<template>
  <main class="admin-center-page" aria-labelledby="admin-center-title">
    <div class="admin-center-page__inner">
      <RouterLink class="back-link" to="/">← 返回首页</RouterLink>

      <SectionHeader>管理中心</SectionHeader>

      <section class="admin-center-placeholder" aria-describedby="admin-center-description">
        <CornerOrnaments color="var(--color-gold-border)" :size="16" />

        <div class="admin-center-placeholder__content">
          <div class="admin-center-placeholder__kicker">ADMIN CONSOLE · FOUNDATION</div>
          <div class="admin-center-placeholder__mark" aria-hidden="true">✦</div>
          <h1 id="admin-center-title">管理中心已建立</h1>
          <p id="admin-center-description">
            管理中心页面视觉将在 Figma Make 完成后接入，当前已准备管理员主播分页数据接口。
          </p>
          <span class="admin-center-placeholder__state">PLACEHOLDER · ADMIN API READY</span>
        </div>
      </section>
    </div>
  </main>
</template>
