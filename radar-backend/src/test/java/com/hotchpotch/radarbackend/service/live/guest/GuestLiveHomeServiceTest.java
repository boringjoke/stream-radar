package com.hotchpotch.radarbackend.service.live.guest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hotchpotch.radarbackend.config.GuestLiveDemoProperties;
import com.hotchpotch.radarbackend.domain.enums.LivePlatform;
import com.hotchpotch.radarbackend.domain.enums.LiveStatus;
import com.hotchpotch.radarbackend.service.live.source.LiveDataSourceRouter;
import com.hotchpotch.radarbackend.service.live.source.LiveSnapshot;
import com.hotchpotch.radarbackend.service.live.url.LiveRoomUrlResolver;
import com.hotchpotch.radarbackend.service.live.url.ResolvedLiveRoom;
import com.hotchpotch.radarbackend.service.live.validation.LiveRoomCheckResult;
import com.hotchpotch.radarbackend.vo.live.LiveAnchorCardVO;
import com.hotchpotch.radarbackend.vo.live.LiveHomeVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * 游客首页四个平台真实演示主播数据定向验证。
 */
class GuestLiveHomeServiceTest {

    @Mock
    private LiveRoomUrlResolver liveRoomUrlResolver;

    @Mock
    private LiveDataSourceRouter liveDataSourceRouter;

    @Mock
    private GuestLiveSseConnectionRegistry connectionRegistry;

    private GuestLiveDemoProperties properties;
    private GuestLiveHomeService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        properties = new GuestLiveDemoProperties();
        ResolvedLiveRoom bilibili = room(LivePlatform.BILIBILI, "22637261");
        ResolvedLiveRoom douyu = room(LivePlatform.DOUYU, "9999");
        ResolvedLiveRoom huya = room(LivePlatform.HUYA, "998");
        ResolvedLiveRoom douyin = room(LivePlatform.DOUYIN, "690434662");
        when(liveRoomUrlResolver.resolve(properties.getBilibiliUrl())).thenReturn(bilibili);
        when(liveRoomUrlResolver.resolve(properties.getDouyuUrl())).thenReturn(douyu);
        when(liveRoomUrlResolver.resolve(properties.getHuyaUrl())).thenReturn(huya);
        when(liveRoomUrlResolver.resolve(properties.getDouyinUrl())).thenReturn(douyin);
        when(liveDataSourceRouter.check(any(ResolvedLiveRoom.class))).thenAnswer(invocation -> {
            ResolvedLiveRoom room = invocation.getArgument(0);
            return LiveRoomCheckResult.available(snapshot(room.getPlatform(), room.getRoomId()));
        });
        service = new GuestLiveHomeService(
                properties,
                liveRoomUrlResolver,
                liveDataSourceRouter,
                connectionRegistry);
    }

    @Test
    void shouldBuildFourRealDemoCardsWithoutFollowIdentity() {
        LiveHomeVO home = service.getHome();

        assertEquals(4, home.getTotalCount());
        assertEquals(4, home.getLiveCount());
        assertEquals(4, home.getAnchors().size());
        assertEquals("BILIBILI", home.getAnchors().get(0).getPlatform());
        assertEquals("DOUYU", home.getAnchors().get(1).getPlatform());
        assertEquals("HUYA", home.getAnchors().get(2).getPlatform());
        assertEquals("DOUYIN", home.getAnchors().get(3).getPlatform());
        for (LiveAnchorCardVO card : home.getAnchors()) {
            assertNull(card.getFollowId());
            assertNull(card.getAnchorId());
            assertEquals(LiveStatus.LIVE.getCode(), card.getLiveStatus());
        }
        verify(connectionRegistry).publish(home);
    }

    @Test
    void shouldKeepPreviousProfileAndMarkErrorWhenSourceFails() {
        service.getHome();
        when(liveDataSourceRouter.check(any(ResolvedLiveRoom.class)))
                .thenReturn(LiveRoomCheckResult.of(
                        com.hotchpotch.radarbackend.service.live.validation.LiveRoomCheckStatus
                                .TEMPORARILY_UNAVAILABLE,
                        "数据源暂时不可用"));

        LiveHomeVO home = service.refresh();

        assertEquals(4, home.getAnchors().size());
        assertEquals(LiveStatus.ERROR.getCode(), home.getAnchors().get(0).getLiveStatus());
        assertEquals("BILIBILI主播", home.getAnchors().get(0).getAnchorName());
        assertEquals("BILIBILI标题", home.getAnchors().get(0).getLiveTitle());
    }

    private ResolvedLiveRoom room(LivePlatform platform, String roomId) {
        return new ResolvedLiveRoom(platform, roomId, platform.getCanonicalUrlPrefix() + "/" + roomId);
    }

    private LiveSnapshot snapshot(LivePlatform platform, String roomId) {
        return new LiveSnapshot(
                platform,
                roomId,
                platform.getCode() + "-uid",
                platform.getCode() + "主播",
                platform.getCode() + "-avatar",
                platform.getCode() + "-cover",
                platform.getCode() + "标题",
                1234L,
                LiveStatus.LIVE);
    }
}
