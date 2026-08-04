package com.hotchpotch.radarbackend.service.live.url;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hotchpotch.radarbackend.common.exception.BusinessException;
import com.hotchpotch.radarbackend.config.RadarUrlProperties;
import com.hotchpotch.radarbackend.domain.enums.LivePlatform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 直播间 URL 纯解析定向验证。
 */
class LiveRoomUrlResolverTest {

    private LiveRoomUrlResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new LiveRoomUrlResolver(new RadarUrlProperties(), WebClient.builder().build());
    }

    @Test
    void shouldResolveDesktopAndMobileRoomUrls() {
        ResolvedLiveRoom bilibili = resolver.resolve("https://live.bilibili.com/22637261?from=share");
        ResolvedLiveRoom douyu = resolver.resolve("https://m.douyu.com/9999/");
        ResolvedLiveRoom huya = resolver.resolve("https://m.huya.com/room/Miss_01/");
        ResolvedLiveRoom douyin = resolver.resolve("https://live.douyin.com/369324308707");

        assertEquals(LivePlatform.BILIBILI, bilibili.getPlatform());
        assertEquals("22637261", bilibili.getRoomId());
        assertEquals("https://live.bilibili.com/22637261", bilibili.getRoomUrl());
        assertEquals(LivePlatform.DOUYU, douyu.getPlatform());
        assertEquals("9999", douyu.getRoomId());
        assertEquals("https://www.douyu.com/9999", douyu.getRoomUrl());
        assertEquals(LivePlatform.HUYA, huya.getPlatform());
        assertEquals("Miss_01", huya.getRoomId());
        assertEquals("https://www.huya.com/Miss_01", huya.getRoomUrl());
        assertEquals(LivePlatform.DOUYIN, douyin.getPlatform());
        assertEquals("369324308707", douyin.getRoomId());
        assertEquals("https://live.douyin.com/369324308707", douyin.getRoomUrl());
    }

    @Test
    void shouldRejectDangerousProtocol() {
        assertThrows(BusinessException.class, () -> resolver.resolve("javascript:alert(1)"));
        assertThrows(BusinessException.class, () -> resolver.resolve("data:text/plain,stream"));
        assertThrows(BusinessException.class, () -> resolver.resolve("file:///etc/passwd"));
    }

    @Test
    void shouldRejectNonTargetHostAndMissingRoomId() {
        assertThrows(
                BusinessException.class,
                () -> resolver.resolve("https://live.bilibili.com.evil.example/22637261"));
        assertThrows(
                BusinessException.class,
                () -> resolver.resolve("https://live.bilibili.com/"));
        assertThrows(
                BusinessException.class,
                () -> resolver.resolve("https://www.douyu.com/%2F9999"));
    }
}
