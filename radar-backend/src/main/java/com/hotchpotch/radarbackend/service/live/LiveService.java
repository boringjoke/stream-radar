package com.hotchpotch.radarbackend.service.live;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.time.LocalDateTime;

import com.hotchpotch.radarbackend.common.exception.BusinessException;
import com.hotchpotch.radarbackend.common.exception.ErrorCode;
import com.hotchpotch.radarbackend.domain.entity.LiveAnchor;
import com.hotchpotch.radarbackend.domain.entity.UserFollowAnchor;
import com.hotchpotch.radarbackend.domain.enums.LiveStatus;
import com.hotchpotch.radarbackend.domain.repository.LiveAnchorRepository;
import com.hotchpotch.radarbackend.domain.repository.UserFollowAnchorRepository;
import com.hotchpotch.radarbackend.request.live.LiveFollowRequest;
import com.hotchpotch.radarbackend.request.live.LiveUnfollowBatchRequest;
import com.hotchpotch.radarbackend.request.live.LiveUnfollowRequest;
import com.hotchpotch.radarbackend.security.RadarUserPrincipal;
import com.hotchpotch.radarbackend.service.live.url.LiveRoomUrlResolver;
import com.hotchpotch.radarbackend.service.live.url.ResolvedLiveRoom;
import com.hotchpotch.radarbackend.service.live.validation.LiveRoomAvailabilityChecker;
import com.hotchpotch.radarbackend.service.live.validation.LiveRoomCheckResult;
import com.hotchpotch.radarbackend.service.live.validation.LiveRoomCheckStatus;
import com.hotchpotch.radarbackend.service.live.source.LiveSnapshot;
import com.hotchpotch.radarbackend.vo.live.LiveAnchorCardVO;
import com.hotchpotch.radarbackend.vo.live.LiveHomeVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 直播首页和关注管理业务服务。
 */
@Service
public class LiveService {

    /**
     * 当前用户默认最多关注的主播数量。
     */
    private final int followLimit;

    /**
     * 直播间 URL 安全解析服务。
     */
    private final LiveRoomUrlResolver liveRoomUrlResolver;

    /**
     * 主播数据访问仓库。
     */
    private final LiveAnchorRepository liveAnchorRepository;

    /**
     * 用户关注关系数据访问仓库。
     */
    private final UserFollowAnchorRepository userFollowAnchorRepository;

    /**
     * 直播间真实存在性校验器。
     */
    private final List<LiveRoomAvailabilityChecker> availabilityCheckers;

    /**
     * 创建直播业务服务。
     *
     * @param followLimit 单用户关注上限
     * @param liveRoomUrlResolver URL 安全解析服务
     * @param liveAnchorRepository 主播数据访问仓库
     * @param userFollowAnchorRepository 关注关系数据访问仓库
     * @param availabilityCheckers 可选的直播间存在性校验器
     */
    public LiveService(
            @org.springframework.beans.factory.annotation.Value("${radar.monitor.follow-limit:100}") int followLimit,
            LiveRoomUrlResolver liveRoomUrlResolver,
            LiveAnchorRepository liveAnchorRepository,
            UserFollowAnchorRepository userFollowAnchorRepository,
            List<LiveRoomAvailabilityChecker> availabilityCheckers) {
        this.followLimit = followLimit;
        this.liveRoomUrlResolver = liveRoomUrlResolver;
        this.liveAnchorRepository = liveAnchorRepository;
        this.userFollowAnchorRepository = userFollowAnchorRepository;
        this.availabilityCheckers = availabilityCheckers == null ? List.of() : availabilityCheckers;
    }

    /**
     * 查询当前用户的关注主播首页数据。
     *
     * @param authentication 当前认证对象
     * @return 当前用户首页数据
     */
    public LiveHomeVO getHome(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        List<LiveAnchorCardVO> anchors = userFollowAnchorRepository.findLiveAnchorCardsByUserId(userId);
        long liveCount = anchors.stream()
                .filter(anchor -> LiveStatus.LIVE.getCode().equals(anchor.getLiveStatus()))
                .count();
        return new LiveHomeVO(anchors.size(), liveCount, anchors);
    }

    /**
     * 解析直播间 URL，创建或复用主播并建立当前用户关注关系。
     *
     * @param authentication 当前认证对象
     * @param request 关注请求
     * @return 新增关注对应的主播卡片
     */
    @Transactional
    public LiveAnchorCardVO follow(Authentication authentication, LiveFollowRequest request) {
        Long userId = getCurrentUserId(authentication);
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "关注请求不能为空");
        }

        ResolvedLiveRoom room = liveRoomUrlResolver.resolve(request.getRoomUrl());
        LiveRoomCheckResult availabilityResult = validateRoomAvailability(room);
        LiveSnapshot snapshot = availabilityResult == null ? null : availabilityResult.getSnapshot();
        String resolvedRoomId = snapshot == null || isBlank(snapshot.getRoomId())
                ? room.getRoomId()
                : snapshot.getRoomId();

        LiveAnchor anchor = liveAnchorRepository
                .findByPlatformAndRoomId(room.getPlatform().getCode(), resolvedRoomId)
                .orElse(null);
        if (anchor == null && !resolvedRoomId.equals(room.getRoomId())) {
            // 兼容此前按短房间号保存的旧记录，并在成功同步时归一化为长房间号。
            anchor = liveAnchorRepository
                    .findByPlatformAndRoomId(room.getPlatform().getCode(), room.getRoomId())
                    .orElse(null);
        }
        if (anchor != null && userFollowAnchorRepository
                .findByUserIdAndAnchorId(userId, anchor.getId()).isPresent()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "已经关注该主播，无需重复添加");
        }

        ensureFollowLimit(userId);
        if (anchor == null) {
            anchor = createOrReuseAnchor(room, snapshot);
        } else if (snapshot != null) {
            applySnapshot(anchor, room, snapshot);
            liveAnchorRepository.updateById(anchor);
        }

        UserFollowAnchor relation = new UserFollowAnchor();
        relation.setUserId(userId);
        relation.setAnchorId(anchor.getId());
        try {
            userFollowAnchorRepository.insert(relation);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "已经关注该主播，无需重复添加", exception);
        }
        return toCard(relation.getId(), anchor);
    }

    /**
     * 取消当前用户的一条关注关系。
     *
     * @param authentication 当前认证对象
     * @param request 取消关注请求
     */
    @Transactional
    public void unfollow(Authentication authentication, LiveUnfollowRequest request) {
        Long userId = getCurrentUserId(authentication);
        if (request == null || request.getFollowId() == null) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "关注关系标识不能为空");
        }

        UserFollowAnchor relation = userFollowAnchorRepository.findById(request.getFollowId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_ERROR, "关注关系不存在或已取消"));
        assertRelationOwner(userId, relation);
        int affectedRows = userFollowAnchorRepository.deleteByUserIdAndId(userId, request.getFollowId());
        if (affectedRows != 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "关注关系不存在或已取消");
        }
    }

    /**
     * 批量取消当前用户的关注关系。
     *
     * @param authentication 当前认证对象
     * @param request 批量取消关注请求
     */
    @Transactional
    public void unfollowBatch(Authentication authentication, LiveUnfollowBatchRequest request) {
        Long userId = getCurrentUserId(authentication);
        if (request == null || request.getFollowIds() == null || request.getFollowIds().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "至少选择一位主播");
        }

        Set<Long> followIds = new LinkedHashSet<>(request.getFollowIds());
        if (followIds.size() != request.getFollowIds().size()) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "关注关系标识不能重复");
        }

        List<UserFollowAnchor> relations = userFollowAnchorRepository.findByIds(followIds);
        Map<Long, UserFollowAnchor> relationMap = new LinkedHashMap<>();
        for (UserFollowAnchor relation : relations) {
            relationMap.put(relation.getId(), relation);
        }
        if (relationMap.size() != followIds.size()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "部分关注关系不存在或已取消");
        }
        relationMap.values().forEach(relation -> assertRelationOwner(userId, relation));

        int affectedRows = userFollowAnchorRepository.deleteByUserIdAndIds(userId, followIds);
        if (affectedRows != followIds.size()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "部分关注关系不存在或已取消");
        }
    }

    /**
     * 校验数据源结果。尚未接入的平台没有对应校验器时保留原有 UNKNOWN 兼容行为；
     * 已接入平台返回 NOT_FOUND 或无法确认时，统一在保存关注关系前拒绝。
     *
     * @param room URL 解析结果
     */
    private LiveRoomCheckResult validateRoomAvailability(ResolvedLiveRoom room) {
        LiveRoomAvailabilityChecker checker = availabilityCheckers.stream()
                .filter(item -> item.supports(room))
                .findFirst()
                .orElse(null);
        if (checker == null) {
            return null;
        }

        LiveRoomCheckResult result = checker.check(room);
        if (result == null || result.getStatus() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "暂时无法确认直播间是否存在，请稍后重试");
        }
        if (result.getStatus() == LiveRoomCheckStatus.NOT_FOUND) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "直播间不存在，无法保存");
        }
        if (result.getStatus() != LiveRoomCheckStatus.AVAILABLE) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "暂时无法确认直播间是否存在，请稍后重试");
        }
        if (result.getSnapshot() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "暂时无法确认直播间资料，请稍后重试");
        }
        return result;
    }

    /**
     * 校验当前用户的关注数量上限。
     *
     * @param userId 当前用户主键
     */
    private void ensureFollowLimit(Long userId) {
        if (followLimit > 0 && userFollowAnchorRepository.countByUserId(userId) >= followLimit) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "已达到关注上限，请先取消部分关注");
        }
    }

    /**
     * 创建主播记录；遇到并发唯一索引冲突时重新读取已有记录复用。
     *
     * @param room URL 解析结果
     * @return 主播实体
     */
    private LiveAnchor createOrReuseAnchor(ResolvedLiveRoom room, LiveSnapshot snapshot) {
        LiveAnchor anchor = new LiveAnchor();
        anchor.setPlatform(room.getPlatform().getCode());
        anchor.setRoomId(resolveRoomId(room, snapshot));
        anchor.setRoomUrl(resolveRoomUrl(room, snapshot));
        if (snapshot == null) {
            anchor.setLiveStatus(LiveStatus.UNKNOWN.getCode());
        } else {
            applySnapshot(anchor, room, snapshot);
        }
        anchor.setFailureCount(0);
        try {
            liveAnchorRepository.insert(anchor);
            return anchor;
        } catch (DuplicateKeyException exception) {
            LiveAnchor existingAnchor = liveAnchorRepository
                    .findByPlatformAndRoomId(room.getPlatform().getCode(), anchor.getRoomId())
                    .or(() -> liveAnchorRepository.findByPlatformAndRoomId(
                            room.getPlatform().getCode(), room.getRoomId()))
                    .orElseThrow(() -> new BusinessException(
                            ErrorCode.BUSINESS_ERROR,
                            "直播间正在被其他请求处理，请稍后重试",
                            exception));
            if (snapshot != null) {
                applySnapshot(existingAnchor, room, snapshot);
                liveAnchorRepository.updateById(existingAnchor);
            }
            return existingAnchor;
        }
    }

    /**
     * 将数据源快照同步到主播实体。
     *
     * @param anchor 主播实体
     * @param room URL 解析结果
     * @param snapshot 数据源快照
     */
    private void applySnapshot(LiveAnchor anchor, ResolvedLiveRoom room, LiveSnapshot snapshot) {
        String previousStatus = anchor.getLiveStatus();
        String resolvedRoomId = resolveRoomId(room, snapshot);
        anchor.setPlatform(room.getPlatform().getCode());
        anchor.setRoomId(resolvedRoomId);
        anchor.setRoomUrl(resolveRoomUrl(room, snapshot));
        if (!isBlank(snapshot.getPlatformUid())) {
            anchor.setPlatformUid(snapshot.getPlatformUid());
        }
        if (!isBlank(snapshot.getAnchorName())) {
            anchor.setAnchorName(snapshot.getAnchorName());
        }
        if (!isBlank(snapshot.getAvatarUrl())) {
            anchor.setAvatarUrl(snapshot.getAvatarUrl());
        }
        if (!isBlank(snapshot.getCoverUrl())) {
            anchor.setCoverUrl(snapshot.getCoverUrl());
        }
        if (!isBlank(snapshot.getLiveTitle())) {
            anchor.setLiveTitle(snapshot.getLiveTitle());
        }
        if (snapshot.getOnlineCount() != null) {
            anchor.setOnlineCount(snapshot.getOnlineCount());
        }
        String nextStatus = snapshot.getLiveStatus().getCode();
        anchor.setLiveStatus(nextStatus);
        anchor.setFailureCount(0);
        anchor.setErrorMessage(null);
        LocalDateTime now = LocalDateTime.now();
        anchor.setLastCheckTime(now);
        anchor.setLastSuccessTime(now);
        if (!Objects.equals(previousStatus, nextStatus)) {
            anchor.setStatusChangeTime(now);
        }
    }

    /**
     * 获取数据源返回的规范房间标识。
     *
     * @param room URL 解析结果
     * @param snapshot 数据源快照
     * @return 规范房间标识
     */
    private String resolveRoomId(ResolvedLiveRoom room, LiveSnapshot snapshot) {
        return snapshot == null || isBlank(snapshot.getRoomId())
                ? room.getRoomId()
                : snapshot.getRoomId();
    }

    /**
     * 获取规范直播间地址。
     *
     * @param room URL 解析结果
     * @param snapshot 数据源快照
     * @return 规范直播间地址
     */
    private String resolveRoomUrl(ResolvedLiveRoom room, LiveSnapshot snapshot) {
        return room.getPlatform().getCanonicalUrlPrefix() + "/" + resolveRoomId(room, snapshot);
    }

    /**
     * 判断字符串是否为空。
     *
     * @param value 待判断文本
     * @return 是否为空
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 校验关注关系属于当前用户。
     *
     * @param userId 当前用户主键
     * @param relation 关注关系
     */
    private void assertRelationOwner(Long userId, UserFollowAnchor relation) {
        if (!Objects.equals(userId, relation.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作其他用户的关注关系");
        }
    }

    /**
     * 从当前认证主体取得用户主键。
     *
     * @param authentication 当前认证对象
     * @return 当前用户主键
     */
    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof RadarUserPrincipal principal)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return principal.getUserId();
    }

    /**
     * 将关注关系和主播实体转换为首页卡片。
     *
     * @param followId 关注关系主键
     * @param anchor 主播实体
     * @return 主播卡片
     */
    private LiveAnchorCardVO toCard(Long followId, LiveAnchor anchor) {
        LiveAnchorCardVO card = new LiveAnchorCardVO();
        card.setFollowId(followId);
        card.setAnchorId(anchor.getId());
        card.setPlatform(anchor.getPlatform());
        card.setRoomId(anchor.getRoomId());
        card.setPlatformUid(anchor.getPlatformUid());
        card.setRoomUrl(anchor.getRoomUrl());
        card.setAnchorName(anchor.getAnchorName());
        card.setAvatarUrl(anchor.getAvatarUrl());
        card.setCoverUrl(anchor.getCoverUrl());
        card.setLiveTitle(anchor.getLiveTitle());
        card.setOnlineCount(anchor.getOnlineCount());
        card.setLiveStatus(anchor.getLiveStatus());
        card.setLastCheckTime(anchor.getLastCheckTime());
        return card;
    }
}
