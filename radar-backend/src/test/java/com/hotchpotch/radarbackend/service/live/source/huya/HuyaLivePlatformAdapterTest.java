package com.hotchpotch.radarbackend.service.live.source.huya;

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
 * 虎牙主备数据源路由定向验证。
 */
class HuyaLivePlatformAdapterTest {

    @Mock
    private HuyaPageProvider pageProvider;

    @Mock
    private HuyaDomProvider domProvider;

    private HuyaLivePlatformAdapter adapter;
    private ResolvedLiveRoom room;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        adapter = new HuyaLivePlatformAdapter(pageProvider, domProvider);
        room = new ResolvedLiveRoom(
                LivePlatform.HUYA,
                "998",
                "https://www.huya.com/998");
    }

    @Test
    void shouldUseDomFallbackWhenPrimaryCannotBeUsed() {
        when(pageProvider.resolve(room))
                .thenReturn(LiveSourceResult.temporarilyUnavailable("主页面请求失败"));
        when(domProvider.resolve(room)).thenReturn(LiveSourceResult.available(snapshot()));

        LiveSourceResult result = adapter.resolve(room);

        assertEquals(LiveSourceStatus.AVAILABLE, result.getStatus());
        assertEquals(LiveStatus.LIVE, result.getSnapshot().getLiveStatus());
    }

    @Test
    void shouldReturnNotFoundOnlyWhenBothSourcesConfirmIt() {
        when(pageProvider.resolve(room)).thenReturn(LiveSourceResult.notFound());
        when(domProvider.resolve(room)).thenReturn(LiveSourceResult.notFound());

        LiveSourceResult result = adapter.resolve(room);

        assertEquals(LiveSourceStatus.NOT_FOUND, result.getStatus());
    }

    @Test
    void shouldKeepUnknownWhenOneSourceCannotConfirm() {
        when(pageProvider.resolve(room)).thenReturn(LiveSourceResult.unknown("主页面字段缺失"));
        when(domProvider.resolve(room)).thenReturn(LiveSourceResult.notFound());

        LiveSourceResult result = adapter.resolve(room);

        assertEquals(LiveSourceStatus.UNKNOWN, result.getStatus());
    }

    @Test
    void shouldKeepMissingAnchorPageUnknownWhenBothSourcesCannotConfirm() {
        when(pageProvider.resolve(room)).thenReturn(LiveSourceResult.unknown("主页面缺少 stream"));
        when(domProvider.resolve(room)).thenReturn(LiveSourceResult.unknown("备用页面提示找不到主播"));

        LiveSourceResult result = adapter.resolve(room);

        assertEquals(LiveSourceStatus.UNKNOWN, result.getStatus());
    }

    /**
     * 创建虎牙直播快照。
     *
     * @return 测试快照
     */
    private LiveSnapshot snapshot() {
        return new LiveSnapshot(
                LivePlatform.HUYA,
                "998",
                "1001",
                "主播",
                "avatar",
                "cover",
                "标题",
                null,
                LiveStatus.LIVE);
    }
}
