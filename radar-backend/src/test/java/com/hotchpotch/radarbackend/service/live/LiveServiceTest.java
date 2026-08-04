package com.hotchpotch.radarbackend.service.live;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.hotchpotch.radarbackend.common.exception.BusinessException;
import com.hotchpotch.radarbackend.common.exception.ErrorCode;
import com.hotchpotch.radarbackend.domain.entity.LiveAnchor;
import com.hotchpotch.radarbackend.domain.entity.SysUser;
import com.hotchpotch.radarbackend.domain.entity.UserFollowAnchor;
import com.hotchpotch.radarbackend.domain.enums.UserRole;
import com.hotchpotch.radarbackend.domain.repository.LiveAnchorRepository;
import com.hotchpotch.radarbackend.domain.repository.UserFollowAnchorRepository;
import com.hotchpotch.radarbackend.request.live.LiveFollowRequest;
import com.hotchpotch.radarbackend.request.live.LiveUnfollowRequest;
import com.hotchpotch.radarbackend.security.RadarUserPrincipal;
import com.hotchpotch.radarbackend.service.live.url.LiveRoomUrlResolver;
import com.hotchpotch.radarbackend.service.live.url.ResolvedLiveRoom;
import com.hotchpotch.radarbackend.service.live.validation.LiveRoomAvailabilityChecker;
import com.hotchpotch.radarbackend.service.live.validation.LiveRoomCheckResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.mockito.Mockito.mock;

/**
 * 关注保存前校验、重复关注和越权取关定向验证。
 */
class LiveServiceTest {

    @Mock
    private LiveRoomUrlResolver liveRoomUrlResolver;

    @Mock
    private LiveAnchorRepository liveAnchorRepository;

    @Mock
    private UserFollowAnchorRepository userFollowAnchorRepository;

    private List<LiveRoomAvailabilityChecker> availabilityCheckers;

    private LiveService liveService;
    private Authentication authentication;
    private ResolvedLiveRoom resolvedRoom;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        availabilityCheckers = List.of();
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("tester");
        user.setRole(UserRole.USER.getCode());
        user.setStatus(1);
        RadarUserPrincipal principal = RadarUserPrincipal.from(user);
        authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                principal.getAuthorities());
        resolvedRoom = new ResolvedLiveRoom(
                com.hotchpotch.radarbackend.domain.enums.LivePlatform.BILIBILI,
                "22637261",
                "https://live.bilibili.com/22637261");
        when(liveRoomUrlResolver.resolve(any())).thenReturn(resolvedRoom);
        liveService = new LiveService(
                100,
                liveRoomUrlResolver,
                liveAnchorRepository,
                userFollowAnchorRepository,
                availabilityCheckers);
    }

    @Test
    void shouldRejectRoomNotFoundBeforePersisting() {
        LiveRoomAvailabilityChecker checker = mock(LiveRoomAvailabilityChecker.class);
        when(checker.supports(resolvedRoom)).thenReturn(true);
        availabilityCheckers = List.of(checker);
        liveService = new LiveService(
                100,
                liveRoomUrlResolver,
                liveAnchorRepository,
                userFollowAnchorRepository,
                availabilityCheckers);
        when(checker.check(resolvedRoom)).thenReturn(LiveRoomCheckResult.notFound());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> liveService.follow(authentication, followRequest()));

        assertEquals("直播间不存在，无法保存", exception.getMessage());
        verify(liveAnchorRepository, never()).insert(any());
        verify(userFollowAnchorRepository, never()).insert(any());
    }

    @Test
    void shouldRejectDuplicateFollowForSameUser() {
        LiveAnchor anchor = new LiveAnchor();
        anchor.setId(10L);
        anchor.setPlatform("BILIBILI");
        anchor.setRoomId("22637261");
        when(liveAnchorRepository.findByPlatformAndRoomId("BILIBILI", "22637261"))
                .thenReturn(Optional.of(anchor));

        UserFollowAnchor relation = new UserFollowAnchor();
        relation.setId(20L);
        relation.setUserId(1L);
        relation.setAnchorId(10L);
        when(userFollowAnchorRepository.findByUserIdAndAnchorId(1L, 10L))
                .thenReturn(Optional.of(relation));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> liveService.follow(authentication, followRequest()));

        assertEquals(ErrorCode.BUSINESS_ERROR, exception.getErrorCode());
        verify(userFollowAnchorRepository, never()).insert(any());
    }

    @Test
    void shouldRejectUnfollowOwnedByAnotherUser() {
        UserFollowAnchor relation = new UserFollowAnchor();
        relation.setId(20L);
        relation.setUserId(2L);
        relation.setAnchorId(10L);
        when(userFollowAnchorRepository.findById(20L)).thenReturn(Optional.of(relation));

        LiveUnfollowRequest request = new LiveUnfollowRequest();
        request.setFollowId(20L);
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> liveService.unfollow(authentication, request));

        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
        verify(userFollowAnchorRepository, never()).deleteByUserIdAndId(any(), any());
    }

    /**
     * 创建测试关注请求。
     *
     * @return 关注请求
     */
    private LiveFollowRequest followRequest() {
        LiveFollowRequest request = new LiveFollowRequest();
        request.setRoomUrl(resolvedRoom.getRoomUrl());
        return request;
    }
}
