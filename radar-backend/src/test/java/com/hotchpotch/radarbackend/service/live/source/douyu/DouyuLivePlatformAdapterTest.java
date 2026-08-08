package com.hotchpotch.radarbackend.service.live.source.douyu;

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
 * 斗鱼主备数据源路由定向验证。
 */
class DouyuLivePlatformAdapterTest {

    @Mock
    private DouyuApiProvider apiProvider;

    @Mock
    private DouyuWebProvider webProvider;

    private DouyuLivePlatformAdapter adapter;
    private ResolvedLiveRoom room;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        adapter = new DouyuLivePlatformAdapter(apiProvider, webProvider);
        room = new ResolvedLiveRoom(
                LivePlatform.DOUYU,
                "9999",
                "https://www.douyu.com/9999");
    }

    @Test
    void shouldUseBackupWhenPrimaryIsTemporarilyUnavailable() {
        when(apiProvider.resolve(room))
                .thenReturn(LiveSourceResult.temporarilyUnavailable("主源超时"));
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
    void shouldKeepUnknownWhenPrimaryCannotConfirmAndBackupSaysNotFound() {
        when(apiProvider.resolve(room)).thenReturn(LiveSourceResult.unknown("主源字段缺失"));
        when(webProvider.resolve(room)).thenReturn(LiveSourceResult.notFound());

        LiveSourceResult result = adapter.resolve(room);

        assertEquals(LiveSourceStatus.UNKNOWN, result.getStatus());
    }

    /**
     * 创建斗鱼直播快照。
     *
     * @return 测试快照
     */
    private LiveSnapshot snapshot() {
        return new LiveSnapshot(
                LivePlatform.DOUYU,
                "9999",
                "204389",
                "主播",
                "avatar",
                "cover",
                "标题",
                null,
                LiveStatus.LIVE);
    }
}
