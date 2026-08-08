package com.hotchpotch.radarbackend.service.live.monitor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.hotchpotch.radarbackend.config.LiveMonitorProperties;
import com.hotchpotch.radarbackend.domain.entity.LiveAnchor;
import com.hotchpotch.radarbackend.domain.enums.LivePlatform;
import com.hotchpotch.radarbackend.domain.enums.LiveStatus;
import com.hotchpotch.radarbackend.domain.repository.LiveAnchorRepository;
import com.hotchpotch.radarbackend.service.live.event.LiveAnchorChangedEvent;
import com.hotchpotch.radarbackend.service.live.event.LiveEventPublisher;
import com.hotchpotch.radarbackend.service.live.source.LiveDataSourceRouter;
import com.hotchpotch.radarbackend.service.live.source.LiveSnapshot;
import com.hotchpotch.radarbackend.service.live.source.LiveSourceResult;
import com.hotchpotch.radarbackend.service.live.source.LiveSourceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 直播主播监控业务服务。
 */
@Service
public class LiveMonitorService {

    /**
     * 日志记录器。
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(LiveMonitorService.class);

    /**
     * 主播数据访问仓库。
     */
    private final LiveAnchorRepository liveAnchorRepository;

    /**
     * 统一数据源路由器。
     */
    private final LiveDataSourceRouter liveDataSourceRouter;

    /**
     * 进程内或未来扩展的数据事件发布器。
     */
    private final LiveEventPublisher liveEventPublisher;

    /**
     * 监控配置。
     */
    private final LiveMonitorProperties properties;

    /**
     * 创建直播监控服务。
     *
     * @param liveAnchorRepository 主播数据访问仓库
     * @param liveDataSourceRouter 统一数据源路由器
     * @param liveEventPublisher 直播事件发布器
     * @param properties 监控配置
     */
    public LiveMonitorService(
            LiveAnchorRepository liveAnchorRepository,
            LiveDataSourceRouter liveDataSourceRouter,
            LiveEventPublisher liveEventPublisher,
            LiveMonitorProperties properties) {
        this.liveAnchorRepository = liveAnchorRepository;
        this.liveDataSourceRouter = liveDataSourceRouter;
        this.liveEventPublisher = liveEventPublisher;
        this.properties = properties;
    }

    /**
     * 执行一次全量去重主播监控。
     *
     * <p>监控对象来自当前仍被有效用户关注的 {@code live_anchor}，同一主播只查询一次。
     * 变化事件在事务提交后发布，避免向 SSE 推送未落库状态。</p>
     */
    @Transactional
    public void monitorOnce() {
        List<LiveAnchor> monitoredAnchors = liveAnchorRepository.findMonitoredLiveAnchors();
        if (monitoredAnchors == null || monitoredAnchors.isEmpty()) {
            return;
        }

        Map<LivePlatform, List<LiveAnchor>> anchorsByPlatform = groupByPlatform(monitoredAnchors);
        List<LiveAnchorChangedEvent> changedEvents = new ArrayList<>();
        for (Map.Entry<LivePlatform, List<LiveAnchor>> entry : anchorsByPlatform.entrySet()) {
            LivePlatform platform = entry.getKey();
            if (!liveDataSourceRouter.supports(platform)) {
                continue;
            }
            List<LiveAnchor> anchors = entry.getValue();
            List<LiveSourceResult> results = liveDataSourceRouter.queryStatus(platform, anchors);
            for (int index = 0; index < anchors.size(); index++) {
                LiveSourceResult result = index < results.size()
                        ? results.get(index)
                        : LiveSourceResult.unknown("平台数据源返回结果数量不足");
                LiveAnchorChangedEvent event = processResult(anchors.get(index), result);
                if (event != null) {
                    changedEvents.add(event);
                }
            }
        }
        publishAfterCommit(changedEvents);
    }

    /**
     * 按平台分组监控主播。
     *
     * @param anchors 去重后的主播列表
     * @return 按平台分组的主播列表
     */
    private Map<LivePlatform, List<LiveAnchor>> groupByPlatform(List<LiveAnchor> anchors) {
        Map<LivePlatform, List<LiveAnchor>> grouped = new LinkedHashMap<>();
        for (LiveAnchor anchor : anchors) {
            if (anchor == null) {
                continue;
            }
            LivePlatform.fromCode(anchor.getPlatform())
                    .ifPresent(platform -> grouped.computeIfAbsent(platform, key -> new ArrayList<>())
                            .add(anchor));
        }
        return grouped;
    }

    /**
     * 处理一条数据源结果，并按变化检测策略决定是否写库。
     *
     * @param anchor 已加载的主播实体
     * @param result 数据源结果
     * @return 需要在事务提交后发布的事件，无语义变化时返回 null
     */
    private LiveAnchorChangedEvent processResult(LiveAnchor anchor, LiveSourceResult result) {
        if (anchor == null) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        if (result != null && result.isAvailable()) {
            return processAvailable(anchor, result.getSnapshot(), now);
        }

        LiveStatus failureStatus = result == null
                ? LiveStatus.UNKNOWN
                : toFailureStatus(result.getStatus());
        String message = result == null || result.getMessage() == null
                ? "平台数据源暂时无法确认主播状态"
                : result.getMessage();
        boolean statusChanged = !Objects.equals(anchor.getLiveStatus(), failureStatus.getCode());
        boolean healthDue = isHealthPersistenceDue(anchor, now);
        if (!statusChanged && !healthDue) {
            return null;
        }

        applyFailure(anchor, failureStatus, message, now, statusChanged);
        liveAnchorRepository.updateById(anchor);
        return statusChanged ? toChangedEvent(anchor) : null;
    }

    /**
     * 处理数据源成功结果。
     *
     * @param anchor 已加载的主播实体
     * @param snapshot 数据源快照
     * @param now 当前时间
     * @return 变化事件或 null
     */
    private LiveAnchorChangedEvent processAvailable(
            LiveAnchor anchor,
            LiveSnapshot snapshot,
            LocalDateTime now) {
        if (snapshot == null) {
            return processResult(anchor, LiveSourceResult.unknown("平台数据源快照为空"));
        }

        boolean semanticChanged = hasSemanticChange(anchor, snapshot);
        boolean healthDue = isHealthPersistenceDue(anchor, now);
        if (!semanticChanged && !healthDue) {
            return null;
        }

        String previousStatus = anchor.getLiveStatus();
        applySuccess(anchor, snapshot, now, previousStatus);
        liveAnchorRepository.updateById(anchor);
        return semanticChanged ? toChangedEvent(anchor) : null;
    }

    /**
     * 判断标准化资料或状态是否发生语义变化。
     *
     * @param anchor 已保存主播
     * @param snapshot 最新快照
     * @return 是否发生语义变化
     */
    private boolean hasSemanticChange(LiveAnchor anchor, LiveSnapshot snapshot) {
        String nextRoomId = firstNonBlank(snapshot.getRoomId(), anchor.getRoomId());
        String nextRoomUrl = buildRoomUrl(snapshot, nextRoomId, anchor.getRoomUrl());
        return !Objects.equals(anchor.getPlatform(), snapshot.getPlatform().getCode())
                || !Objects.equals(anchor.getRoomId(), nextRoomId)
                || !Objects.equals(anchor.getRoomUrl(), nextRoomUrl)
                || changedWhenPresent(anchor.getPlatformUid(), snapshot.getPlatformUid())
                || changedWhenPresent(anchor.getAnchorName(), snapshot.getAnchorName())
                || changedWhenPresent(anchor.getAvatarUrl(), snapshot.getAvatarUrl())
                || changedWhenPresent(anchor.getCoverUrl(), snapshot.getCoverUrl())
                || changedWhenPresent(anchor.getLiveTitle(), snapshot.getLiveTitle())
                || (snapshot.getOnlineCount() != null
                && !Objects.equals(anchor.getOnlineCount(), snapshot.getOnlineCount()))
                || !Objects.equals(anchor.getLiveStatus(), snapshot.getLiveStatus().getCode());
    }

    /**
     * 将成功快照应用到主播实体。
     *
     * @param anchor 主播实体
     * @param snapshot 数据源快照
     * @param now 当前时间
     * @param previousStatus 更新前状态
     */
    private void applySuccess(
            LiveAnchor anchor,
            LiveSnapshot snapshot,
            LocalDateTime now,
            String previousStatus) {
        String roomId = firstNonBlank(snapshot.getRoomId(), anchor.getRoomId());
        anchor.setPlatform(snapshot.getPlatform().getCode());
        anchor.setRoomId(roomId);
        anchor.setRoomUrl(buildRoomUrl(snapshot, roomId, anchor.getRoomUrl()));
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
        anchor.setLiveStatus(snapshot.getLiveStatus().getCode());
        anchor.setFailureCount(0);
        anchor.setErrorMessage(null);
        anchor.setLastCheckTime(now);
        anchor.setLastSuccessTime(now);
        if (!Objects.equals(previousStatus, anchor.getLiveStatus())) {
            anchor.setStatusChangeTime(now);
        }
    }

    /**
     * 将数据源失败应用到主播实体；失败不会被写成 OFFLINE。
     *
     * @param anchor 主播实体
     * @param status 失败后展示状态
     * @param message 错误摘要
     * @param now 当前时间
     * @param statusChanged 状态是否发生变化
     */
    private void applyFailure(
            LiveAnchor anchor,
            LiveStatus status,
            String message,
            LocalDateTime now,
            boolean statusChanged) {
        int failureCount = anchor.getFailureCount() == null ? 0 : anchor.getFailureCount();
        anchor.setFailureCount(failureCount == Integer.MAX_VALUE ? Integer.MAX_VALUE : failureCount + 1);
        anchor.setErrorMessage(limitMessage(message));
        anchor.setLiveStatus(status.getCode());
        anchor.setLastCheckTime(now);
        if (statusChanged) {
            anchor.setStatusChangeTime(now);
        }
    }

    /**
     * 将内部失败类型转换为数据库展示状态。
     *
     * @param status 数据源状态
     * @return 失败展示状态
     */
    private LiveStatus toFailureStatus(LiveSourceStatus status) {
        return switch (status == null ? LiveSourceStatus.UNKNOWN : status) {
            case NOT_FOUND, TEMPORARILY_UNAVAILABLE -> LiveStatus.ERROR;
            case UNKNOWN, AVAILABLE -> LiveStatus.UNKNOWN;
        };
    }

    /**
     * 判断健康字段是否达到限频持久化时间。
     *
     * @param anchor 主播实体
     * @param now 当前时间
     * @return 是否应持久化健康字段
     */
    private boolean isHealthPersistenceDue(LiveAnchor anchor, LocalDateTime now) {
        if (anchor.getLastCheckTime() == null) {
            return true;
        }
        long intervalMs = Math.max(0L, properties.getHealthPersistIntervalMs());
        if (intervalMs == 0L) {
            return true;
        }
        Duration elapsed = Duration.between(anchor.getLastCheckTime(), now);
        return elapsed.isNegative() || elapsed.toMillis() >= intervalMs;
    }

    /**
     * 创建已落库的内部变化事件。
     *
     * @param anchor 已更新主播
     * @return 主播变化事件
     */
    private LiveAnchorChangedEvent toChangedEvent(LiveAnchor anchor) {
        return new LiveAnchorChangedEvent(
                anchor.getId(),
                snapshotFromAnchor(anchor),
                anchor.getRoomUrl(),
                anchor.getLastCheckTime());
    }

    /**
     * 从主播实体恢复统一快照。
     *
     * @param anchor 主播实体
     * @return 统一主播快照
     */
    private LiveSnapshot snapshotFromAnchor(LiveAnchor anchor) {
        LivePlatform platform = LivePlatform.fromCode(anchor.getPlatform()).orElse(LivePlatform.BILIBILI);
        LiveStatus status = toLiveStatus(anchor.getLiveStatus());
        return new LiveSnapshot(
                platform,
                anchor.getRoomId(),
                anchor.getPlatformUid(),
                anchor.getAnchorName(),
                anchor.getAvatarUrl(),
                anchor.getCoverUrl(),
                anchor.getLiveTitle(),
                anchor.getOnlineCount(),
                status);
    }

    /**
     * 将数据库状态转换为统一状态。
     *
     * @param status 数据库状态
     * @return 统一状态
     */
    private LiveStatus toLiveStatus(String status) {
        for (LiveStatus value : LiveStatus.values()) {
            if (value.getCode().equals(status)) {
                return value;
            }
        }
        return LiveStatus.UNKNOWN;
    }

    /**
     * 事务提交后发布事件。
     *
     * @param events 待发布事件
     */
    private void publishAfterCommit(List<LiveAnchorChangedEvent> events) {
        if (events.isEmpty()) {
            return;
        }
        List<LiveAnchorChangedEvent> immutableEvents = List.copyOf(events);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publishEvents(immutableEvents);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publishEvents(immutableEvents);
            }
        });
    }

    /**
     * 执行事件发布并隔离单个订阅器异常。
     *
     * @param events 待发布事件
     */
    private void publishEvents(List<LiveAnchorChangedEvent> events) {
        for (LiveAnchorChangedEvent event : events) {
            try {
                liveEventPublisher.publish(event);
            } catch (RuntimeException exception) {
                LOGGER.warn("直播变化事件发布失败，anchorId={}", event.getAnchorId(), exception);
            }
        }
    }

    /**
     * 判断仅在快照提供该字段时是否发生变化。
     *
     * @param current 当前字段
     * @param next 快照字段
     * @return 是否发生变化
     */
    private boolean changedWhenPresent(String current, String next) {
        return !isBlank(next) && !Objects.equals(current, next);
    }

    /**
     * 选择第一个非空字符串。
     *
     * @param first 优先值
     * @param fallback 备用值
     * @return 选择结果
     */
    private String firstNonBlank(String first, String fallback) {
        return isBlank(first) ? fallback : first;
    }

    /**
     * 生成规范直播间地址。
     *
     * @param snapshot 数据源快照
     * @param roomId 房间标识
     * @param fallback 原有地址
     * @return 规范直播间地址
     */
    private String buildRoomUrl(LiveSnapshot snapshot, String roomId, String fallback) {
        if (snapshot == null || isBlank(roomId)) {
            return fallback;
        }
        return snapshot.getPlatform().getCanonicalUrlPrefix() + "/" + roomId;
    }

    /**
     * 截断错误摘要，避免超出数据库字段长度。
     *
     * @param message 原始错误摘要
     * @return 截断后的错误摘要
     */
    private String limitMessage(String message) {
        String value = isBlank(message) ? "平台数据源暂时无法确认主播状态" : message.trim();
        int maxLength = Math.min(512, Math.max(1, properties.getMaxErrorMessageLength()));
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    /**
     * 判断文本是否为空。
     *
     * @param value 待判断文本
     * @return 是否为空
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
