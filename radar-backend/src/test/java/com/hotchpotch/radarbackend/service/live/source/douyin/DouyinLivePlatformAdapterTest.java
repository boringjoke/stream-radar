package com.hotchpotch.radarbackend.service.live.source.douyin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.hotchpotch.radarbackend.domain.enums.LivePlatform;
import com.hotchpotch.radarbackend.domain.enums.LiveStatus;
import com.hotchpotch.radarbackend.service.live.source.LiveSnapshot;
import com.hotchpotch.radarbackend.service.live.source.LiveSourceResult;
import com.hotchpotch.radarbackend.service.live.source.LiveSourceStatus;
import com.hotchpotch.radarbackend.service.live.url.ResolvedLiveRoom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * 抖音主备数据源路由定向验证。
 */
class DouyinLivePlatformAdapterTest {

    @Mock
    private DouyinApiProvider apiProvider;

    @Mock
    private DouyinWebProvider webProvider;

    private DouyinLivePlatformAdapter adapter;

    private ResolvedLiveRoom room;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        adapter = new DouyinLivePlatformAdapter(apiProvider, webProvider);
        room = new ResolvedLiveRoom(
                LivePlatform.DOUYIN,
                "998",
                "https://live.douyin.com/998");
    }

    @Test
    void shouldUseWebFallbackWhenApiCannotBeUsed() {
        when(apiProvider.resolve(room))
                .thenReturn(LiveSourceResult.temporarilyUnavailable("主源 Cookie 失效"));
        when(webProvider.resolve(room)).thenReturn(LiveSourceResult.available(snapshot()));

        LiveSourceResult result = adapter.resolve(room);

        assertEquals(LiveSourceStatus.AVAILABLE, result.getStatus());
        assertEquals(LiveStatus.LIVE, result.getSnapshot().getLiveStatus());
    }

    @Test
    void shouldReturnNotFoundOnlyWhenBothSourcesConfirmIt() {
        when(apiProvider.resolve(room)).thenReturn(LiveSourceResult.notFound());
        when(webProvider.resolve(room)).thenReturn(LiveSourceResult.notFound());

        LiveSourceResult result = adapter.resolve(room);

        assertEquals(LiveSourceStatus.NOT_FOUND, result.getStatus());
    }

    @Test
    void shouldKeepUnknownWhenApiCannotConfirmAndWebSaysNotFound() {
        when(apiProvider.resolve(room)).thenReturn(LiveSourceResult.unknown("主源业务错误"));
        when(webProvider.resolve(room)).thenReturn(LiveSourceResult.notFound());

        LiveSourceResult result = adapter.resolve(room);

        assertEquals(LiveSourceStatus.UNKNOWN, result.getStatus());
    }

    @Test
    void shouldKeepTemporaryUnavailableWhenEitherSourceHasNetworkFailure() {
        when(apiProvider.resolve(room))
                .thenReturn(LiveSourceResult.temporarilyUnavailable("主源超时"));
        when(webProvider.resolve(room))
                .thenReturn(LiveSourceResult.temporarilyUnavailable("备源超时"));

        LiveSourceResult result = adapter.resolve(room);

        assertEquals(LiveSourceStatus.TEMPORARILY_UNAVAILABLE, result.getStatus());
    }

    private LiveSnapshot snapshot() {
        return new LiveSnapshot(
                LivePlatform.DOUYIN,
                "998",
                "1001",
                "主播",
                "avatar",
                "cover",
                "标题",
                1234L,
                LiveStatus.LIVE);
    }
}
