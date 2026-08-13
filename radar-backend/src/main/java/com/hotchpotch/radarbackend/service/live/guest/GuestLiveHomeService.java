package com.hotchpotch.radarbackend.service.live.guest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.hotchpotch.radarbackend.config.GuestLiveDemoProperties;
import com.hotchpotch.radarbackend.domain.enums.LivePlatform;
import com.hotchpotch.radarbackend.domain.enums.LiveStatus;
import com.hotchpotch.radarbackend.service.live.source.LiveDataSourceRouter;
import com.hotchpotch.radarbackend.service.live.source.LiveSnapshot;
import com.hotchpotch.radarbackend.service.live.url.LiveRoomUrlResolver;
import com.hotchpotch.radarbackend.service.live.url.ResolvedLiveRoom;
import com.hotchpotch.radarbackend.service.live.validation.LiveRoomCheckResult;
import com.hotchpotch.radarbackend.service.live.validation.LiveRoomCheckStatus;
import com.hotchpotch.radarbackend.vo.live.LiveAnchorCardVO;
import com.hotchpotch.radarbackend.vo.live.LiveHomeVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 游客首页四个平台真实演示主播数据服务。
 *
 * <p>该服务只查询平台数据源并维护进程内快照，不写入主播表和用户关注关系。</p>
 */
@Service
public class GuestLiveHomeService {

    /**
     * 日志记录器。
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(GuestLiveHomeService.class);

    /**
     * 游客演示主播数量。
     */
    private static final int DEMO_ROOM_COUNT = 4;

    /**
     * 游客演示主播配置。
     */
    private final GuestLiveDemoProperties properties;

    /**
     * 直播间 URL 安全解析服务。
     */
    private final LiveRoomUrlResolver liveRoomUrlResolver;

    /**
     * 统一直播数据源路由器。
     */
    private final LiveDataSourceRouter liveDataSourceRouter;

    /**
     * 游客 SSE 注册表。
     */
    private final GuestLiveSseConnectionRegistry connectionRegistry;

    /**
     * 刷新锁，避免首次请求和定时刷新并发访问平台数据源。
     */
    private final Object refreshMonitor = new Object();

    /**
     * 当前游客首页快照。
     */
    private volatile LiveHomeVO currentHome;

    /**
     * 创建游客首页真实数据服务。
     *
     * @param properties 游客演示主播配置
     * @param liveRoomUrlResolver 直播间 URL 安全解析服务
     * @param liveDataSourceRouter 统一直播数据源路由器
     * @param connectionRegistry 游客 SSE 注册表
     */
    public GuestLiveHomeService(
            GuestLiveDemoProperties properties,
            LiveRoomUrlResolver liveRoomUrlResolver,
            LiveDataSourceRouter liveDataSourceRouter,
            GuestLiveSseConnectionRegistry connectionRegistry) {
        this.properties = properties;
        this.liveRoomUrlResolver = liveRoomUrlResolver;
        this.liveDataSourceRouter = liveDataSourceRouter;
        this.connectionRegistry = connectionRegistry;
    }

    /**
     * 获取当前游客首页快照；首次访问时立即查询四个平台。
     *
     * @return 游客首页真实主播快照
     */
    public LiveHomeVO getHome() {
        LiveHomeVO home = currentHome;
        return home == null ? refresh() : home;
    }

    /**
     * 查询四个平台演示主播并按语义变化广播游客 SSE 快照。
     *
     * @return 刷新后的游客首页快照
     */
    public LiveHomeVO refresh() {
        synchronized (refreshMonitor) {
            LiveHomeVO previousHome = currentHome;
            Map<LivePlatform, LiveAnchorCardVO> previousCards = indexCards(previousHome);
            List<LiveAnchorCardVO> cards = new ArrayList<>(DEMO_ROOM_COUNT);
            for (DemoRoomDefinition definition : demoRooms()) {
                cards.add(queryRoom(definition, previousCards.get(definition.platform())));
            }

            long liveCount = cards.stream()
                    .filter(card -> LiveStatus.LIVE.getCode().equals(card.getLiveStatus()))
                    .count();
            LiveHomeVO nextHome = new LiveHomeVO(cards.size(), liveCount, cards);
            currentHome = nextHome;
            if (!sameHome(previousHome, nextHome)) {
                connectionRegistry.publish(nextHome);
            }
            return nextHome;
        }
    }

    /**
     * 按现有监控间隔刷新游客演示主播，但不写入数据库。
     */
    @Scheduled(
            fixedDelayString = "${radar.monitor.fixed-delay-ms:60000}",
            initialDelayString = "${radar.monitor.initial-delay-ms:10000}")
    public void refreshScheduled() {
        refresh();
    }

    /**
     * 查询单个平台演示主播。
     *
     * @param definition 演示主播定义
     * @param previous 上一次卡片
     * @return 当前卡片
     */
    private LiveAnchorCardVO queryRoom(
            DemoRoomDefinition definition,
            LiveAnchorCardVO previous) {
        ResolvedLiveRoom room = null;
        try {
            room = liveRoomUrlResolver.resolve(definition.url());
            LiveRoomCheckResult result = liveDataSourceRouter.check(room);
            if (result != null
                    && result.getStatus() == LiveRoomCheckStatus.AVAILABLE
                    && result.getSnapshot() != null) {
                return toAvailableCard(definition, room, result.getSnapshot(), previous);
            }
            LiveRoomCheckStatus status = result == null
                    ? LiveRoomCheckStatus.UNKNOWN
                    : result.getStatus();
            return toFailureCard(definition, room, previous, toFailureStatus(status));
        } catch (RuntimeException exception) {
            LOGGER.warn("游客演示主播查询失败，platform={}, roomId={}",
                    definition.platform().getCode(), definition.fallbackRoomId());
            return toFailureCard(definition, room, previous, LiveStatus.ERROR);
        }
    }

    /**
     * 将统一平台快照转换为游客卡片。
     *
     * @param definition 演示主播定义
     * @param room URL 解析结果
     * @param snapshot 平台统一快照
     * @param previous 上一次卡片
     * @return 游客主播卡片
     */
    private LiveAnchorCardVO toAvailableCard(
            DemoRoomDefinition definition,
            ResolvedLiveRoom room,
            LiveSnapshot snapshot,
            LiveAnchorCardVO previous) {
        LiveAnchorCardVO card = baseCard(definition, room);
        card.setPlatform(snapshot.getPlatform().getCode());
        card.setRoomId(firstNonBlank(snapshot.getRoomId(), card.getRoomId()));
        card.setPlatformUid(firstNonBlank(snapshot.getPlatformUid(), previousValue(previous, true)));
        card.setAnchorName(firstNonBlank(snapshot.getAnchorName(), previousValue(previous, false)));
        card.setAvatarUrl(firstNonBlank(snapshot.getAvatarUrl(), previous == null ? null : previous.getAvatarUrl()));
        card.setCoverUrl(firstNonBlank(snapshot.getCoverUrl(), previous == null ? null : previous.getCoverUrl()));
        card.setLiveTitle(firstNonBlank(snapshot.getLiveTitle(), previous == null ? null : previous.getLiveTitle()));
        card.setOnlineCount(snapshot.getOnlineCount() == null
                ? previous == null ? null : previous.getOnlineCount()
                : snapshot.getOnlineCount());
        card.setLiveStatus(snapshot.getLiveStatus().getCode());
        return card;
    }

    /**
     * 将失败结果转换为不误判为未开播的游客卡片。
     *
     * @param definition 演示主播定义
     * @param room URL 解析结果
     * @param previous 上一次卡片
     * @param status 失败展示状态
     * @return 游客主播卡片
     */
    private LiveAnchorCardVO toFailureCard(
            DemoRoomDefinition definition,
            ResolvedLiveRoom room,
            LiveAnchorCardVO previous,
            LiveStatus status) {
        LiveAnchorCardVO card = previous == null ? baseCard(definition, room) : copyCard(previous);
        card.setFollowId(null);
        card.setAnchorId(null);
        card.setLiveStatus(status.getCode());
        return card;
    }

    /**
     * 创建游客卡片基础身份字段。
     *
     * @param definition 演示主播定义
     * @param room URL 解析结果
     * @return 基础卡片
     */
    private LiveAnchorCardVO baseCard(DemoRoomDefinition definition, ResolvedLiveRoom room) {
        LiveAnchorCardVO card = new LiveAnchorCardVO();
        card.setFollowId(null);
        card.setAnchorId(null);
        card.setPlatform(definition.platform().getCode());
        card.setRoomId(room == null ? definition.fallbackRoomId() : firstNonBlank(
                room.getRoomId(), definition.fallbackRoomId()));
        card.setRoomUrl(room == null ? definition.url() : room.getRoomUrl());
        card.setLiveStatus(LiveStatus.UNKNOWN.getCode());
        return card;
    }

    /**
     * 建立平台索引，便于数据源暂时异常时保留上一轮资料。
     *
     * @param home 上一轮首页快照
     * @return 平台到卡片的索引
     */
    private Map<LivePlatform, LiveAnchorCardVO> indexCards(LiveHomeVO home) {
        Map<LivePlatform, LiveAnchorCardVO> result = new HashMap<>();
        if (home == null || home.getAnchors() == null) {
            return result;
        }
        for (LiveAnchorCardVO card : home.getAnchors()) {
            if (card == null || card.getPlatform() == null) {
                continue;
            }
            LivePlatform.fromCode(card.getPlatform()).ifPresent(platform -> result.put(platform, card));
        }
        return result;
    }

    /**
     * 判断首页快照是否发生需要广播的语义变化。
     *
     * @param previous 上一轮快照
     * @param next 当前快照
     * @return 是否发生变化
     */
    private boolean sameHome(LiveHomeVO previous, LiveHomeVO next) {
        if (previous == null || next == null
                || previous.getTotalCount() != next.getTotalCount()
                || previous.getLiveCount() != next.getLiveCount()) {
            return false;
        }
        List<LiveAnchorCardVO> previousCards = previous.getAnchors();
        List<LiveAnchorCardVO> nextCards = next.getAnchors();
        if (previousCards == null || nextCards == null || previousCards.size() != nextCards.size()) {
            return false;
        }
        for (int index = 0; index < previousCards.size(); index++) {
            if (!sameCard(previousCards.get(index), nextCards.get(index))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断主播卡片是否发生语义变化。
     *
     * @param previous 上一轮卡片
     * @param next 当前卡片
     * @return 是否相同
     */
    private boolean sameCard(LiveAnchorCardVO previous, LiveAnchorCardVO next) {
        if (previous == null || next == null) {
            return previous == next;
        }
        return Objects.equals(previous.getPlatform(), next.getPlatform())
                && Objects.equals(previous.getRoomId(), next.getRoomId())
                && Objects.equals(previous.getRoomUrl(), next.getRoomUrl())
                && Objects.equals(previous.getPlatformUid(), next.getPlatformUid())
                && Objects.equals(previous.getAnchorName(), next.getAnchorName())
                && Objects.equals(previous.getAvatarUrl(), next.getAvatarUrl())
                && Objects.equals(previous.getCoverUrl(), next.getCoverUrl())
                && Objects.equals(previous.getLiveTitle(), next.getLiveTitle())
                && Objects.equals(previous.getOnlineCount(), next.getOnlineCount())
                && Objects.equals(previous.getLiveStatus(), next.getLiveStatus());
    }

    /**
     * 将存在性校验失败状态转换为卡片状态。
     *
     * @param status 数据源校验状态
     * @return 游客卡片状态
     */
    private LiveStatus toFailureStatus(LiveRoomCheckStatus status) {
        return switch (status == null ? LiveRoomCheckStatus.UNKNOWN : status) {
            case NOT_FOUND, TEMPORARILY_UNAVAILABLE -> LiveStatus.ERROR;
            case UNKNOWN, AVAILABLE -> LiveStatus.UNKNOWN;
        };
    }

    /**
     * 复制游客卡片。
     *
     * @param source 原卡片
     * @return 卡片副本
     */
    private LiveAnchorCardVO copyCard(LiveAnchorCardVO source) {
        LiveAnchorCardVO target = new LiveAnchorCardVO();
        target.setFollowId(source.getFollowId());
        target.setAnchorId(source.getAnchorId());
        target.setPlatform(source.getPlatform());
        target.setRoomId(source.getRoomId());
        target.setPlatformUid(source.getPlatformUid());
        target.setRoomUrl(source.getRoomUrl());
        target.setAnchorName(source.getAnchorName());
        target.setAvatarUrl(source.getAvatarUrl());
        target.setCoverUrl(source.getCoverUrl());
        target.setLiveTitle(source.getLiveTitle());
        target.setOnlineCount(source.getOnlineCount());
        target.setLiveStatus(source.getLiveStatus());
        target.setLastCheckTime(source.getLastCheckTime());
        return target;
    }

    /**
     * 获取上一轮平台主播 UID。
     *
     * @param previous 上一轮卡片
     * @param platformUid 是否读取平台 UID
     * @return 上一轮字段
     */
    private String previousValue(LiveAnchorCardVO previous, boolean platformUid) {
        if (previous == null) {
            return null;
        }
        return platformUid ? previous.getPlatformUid() : previous.getAnchorName();
    }

    /**
     * 获取第一个非空文本。
     *
     * @param first 优先文本
     * @param fallback 备用文本
     * @return 非空文本或 null
     */
    private String firstNonBlank(String first, String fallback) {
        return first == null || first.isBlank() ? fallback : first;
    }

    /**
     * 构造四个平台游客演示主播定义。
     *
     * @return 演示主播定义列表
     */
    private List<DemoRoomDefinition> demoRooms() {
        return List.of(
                new DemoRoomDefinition(LivePlatform.BILIBILI, "22637261", properties.getBilibiliUrl()),
                new DemoRoomDefinition(LivePlatform.DOUYU, "9999", properties.getDouyuUrl()),
                new DemoRoomDefinition(LivePlatform.HUYA, "998", properties.getHuyaUrl()),
                new DemoRoomDefinition(LivePlatform.DOUYIN, "690434662", properties.getDouyinUrl()));
    }

    /**
     * 游客首页固定演示主播定义。
     *
     * @param platform 平台
     * @param fallbackRoomId 配置解析失败时使用的房间标识
     * @param url 直播间地址
     */
    private record DemoRoomDefinition(LivePlatform platform, String fallbackRoomId, String url) {
    }
}
