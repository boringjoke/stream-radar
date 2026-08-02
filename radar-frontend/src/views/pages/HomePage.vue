<script setup lang="ts">
import GoldDivider from '@/components/GoldDivider.vue'
import SectionHeader from '@/components/SectionHeader.vue'
import StreamerCard, { type StreamerCardData } from '@/components/StreamerCard.vue'
import { platformCatalog } from '@/types/platform'

/**
 * Figma Make 的游客首页展示数据。
 *
 * 这些数据只负责让页面在业务接口接入前保持完整视觉，不进入 API 或 Session 数据链路。
 */
const demoStreamers: StreamerCardData[] = [
  {
    id: 'demo-bilibili',
    name: '老番茄',
    platform: 'bilibili',
    status: 'live',
    title: '【周末闲聊】聊聊那些年我们追过的动漫',
    viewers: '12.4万',
    url: 'https://live.bilibili.com/22637261',
  },
  {
    id: 'demo-douyu',
    name: '冯提莫',
    platform: 'douyu',
    status: 'live',
    title: '今晚翻唱经典歌曲合集，等你来听！',
    viewers: '8.6万',
    url: 'https://www.douyu.com/9999',
  },
  {
    id: 'demo-huya',
    name: 'Miss',
    platform: 'huya',
    status: 'offline',
    title: '上次直播：S14全球总决赛 EDG vs T1',
    viewers: null,
    url: 'https://www.huya.com/998',
  },
  {
    id: 'demo-douyin',
    name: '李子柒',
    platform: 'douyin',
    status: 'offline',
    title: '上次直播：古法酿酒，从选粮到封坛',
    viewers: null,
    url: 'https://live.douyin.com/369324308707',
  },
]
</script>

<template>
  <main class="guest-home">
    <section class="guest-hero" aria-labelledby="guest-home-title">
      <div class="guest-hero__inner animate-fade-in-up">
        <div class="guest-hero__ornament">
          <GoldDivider />
        </div>

        <div class="guest-hero__kicker">Cross-Platform Stream Monitor</div>

        <h1 id="guest-home-title">一站掌握全平台直播</h1>

        <p class="guest-hero__description">关注 B站、斗鱼、虎牙、抖音的主播，开播立刻知道</p>

        <div class="platform-pills" aria-label="支持的平台">
          <span v-for="(platform, key) in platformCatalog" :key="key" class="platform-pill">
            <i :style="{ backgroundColor: platform.color }" aria-hidden="true"></i>
            {{ platform.roman }}
          </span>
        </div>

        <RouterLink class="figma-button figma-button--hero" to="/login">登录开始使用</RouterLink>
      </div>
    </section>

    <section class="demo-section" aria-labelledby="demo-section-title">
      <SectionHeader>
        <span id="demo-section-title">平台示例</span>
      </SectionHeader>

      <div class="demo-card-grid">
        <div v-for="(streamer, index) in demoStreamers" :key="streamer.id" class="demo-card-column">
          <StreamerCard :streamer="streamer" :delay="index * 0.08" />
        </div>
      </div>
    </section>
  </main>
</template>
