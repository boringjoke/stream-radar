<script setup lang="ts">
import CornerOrnaments from '@/components/CornerOrnaments.vue'
</script>

<template>
  <main class="user-home">
    <section class="user-home__inner" aria-labelledby="user-home-title">
      <div class="user-home__heading-row animate-fade-in-up">
        <div>
          <div class="user-home__kicker">MY STREAMERS</div>
          <h1 id="user-home-title">关注主播</h1>
        </div>

        <div class="user-home__count" aria-label="当前关注主播数量">
          <strong>0</strong>
          <span>位关注</span>
        </div>
      </div>

      <div class="user-home__rule" aria-hidden="true"></div>

      <section class="user-home__empty-area" aria-labelledby="user-home-empty-title">
        <div class="user-home__empty">
          <CornerOrnaments color="var(--color-gold-border)" :size="14" />
          <div class="user-home__empty-mark" aria-hidden="true">✦</div>
          <h2 id="user-home-empty-title">暂未关注任何主播</h2>
          <p>主播关注管理将在下一阶段开放</p>
        </div>
      </section>
    </section>
  </main>
</template>
