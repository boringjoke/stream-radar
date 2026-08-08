package com.hotchpotch.radarbackend.service.live.sse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentMap;

import com.hotchpotch.radarbackend.config.LiveSseProperties;
import com.hotchpotch.radarbackend.domain.entity.UserFollowAnchor;
import com.hotchpotch.radarbackend.domain.repository.UserFollowAnchorRepository;
import com.hotchpotch.radarbackend.service.live.event.LiveAnchorChangedEvent;
import com.hotchpotch.radarbackend.service.live.event.LiveEventSubscriber;
import com.hotchpotch.radarbackend.service.live.source.LiveSnapshot;
import com.hotchpotch.radarbackend.vo.live.LiveAnchorCardVO;
import com.hotchpotch.radarbackend.vo.live.LiveHomeVO;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 单实例进程内 SSE 连接注册表。
 *
 * <p>连接只保存在当前应用进程内，不写入 Redis；多实例部署时应替换事件分发层，
 * 不跨进程复制 {@link SseEmitter}。</p>
 */
@Component
public class LiveSseConnectionRegistry implements LiveEventSubscriber {

    /**
     * SSE 重连建议间隔，单位毫秒。
     */
    private static final long RECONNECT_DELAY_MS = 3000L;

    /**
     * 按用户隔离的 SSE 连接列表。
     */
    private final ConcurrentMap<Long, CopyOnWriteArrayList<LiveSseConnection>> connections
            = new ConcurrentHashMap<>();

    /**
     * 用户关注关系仓库，用于事件发布时再次校验接收用户。
     */
    private final UserFollowAnchorRepository userFollowAnchorRepository;

    /**
     * SSE 配置。
     */
    private final LiveSseProperties properties;

    /**
     * 创建 SSE 连接注册表。
     *
     * @param userFollowAnchorRepository 用户关注关系仓库
     * @param properties SSE 配置
     */
    public LiveSseConnectionRegistry(
            UserFollowAnchorRepository userFollowAnchorRepository,
            LiveSseProperties properties) {
        this.userFollowAnchorRepository = userFollowAnchorRepository;
        this.properties = properties;
    }

    /**
     * 注册用户连接并发送当前首页快照。
     *
     * @param userId 当前用户主键
     * @param home 当前用户首页快照
     * @return SSE 发射器
     */
    public SseEmitter connect(Long userId, LiveHomeVO home) {
        long timeout = Math.max(0L, properties.getEmitterTimeoutMs());
        SseEmitter emitter = new SseEmitter(timeout);
        LiveSseConnection connection = new LiveSseConnection(userId, emitter);
        emitter.onCompletion(() -> remove(connection));
        emitter.onTimeout(() -> remove(connection));
        emitter.onError(error -> remove(connection));
        connections.computeIfAbsent(userId, key -> new CopyOnWriteArrayList<>()).add(connection);

        if (!connection.send("snapshot", home)) {
            remove(connection);
        }
        return emitter;
    }

    @Override
    public void onLiveAnchorChanged(LiveAnchorChangedEvent event) {
        if (event == null) {
            return;
        }
        List<UserFollowAnchor> relations = userFollowAnchorRepository.findByAnchorId(event.getAnchorId());
        if (relations.isEmpty()) {
            return;
        }

        LiveAnchorCardVO cardTemplate = toCard(event);
        for (UserFollowAnchor relation : relations) {
            if (relation == null || relation.getUserId() == null) {
                continue;
            }
            CopyOnWriteArrayList<LiveSseConnection> userConnections = connections.get(relation.getUserId());
            if (userConnections == null || userConnections.isEmpty()) {
                continue;
            }
            LiveAnchorCardVO card = copyCard(cardTemplate);
            card.setFollowId(relation.getId());
            for (LiveSseConnection connection : userConnections) {
                if (!connection.send("streamer.updated", card)) {
                    remove(connection);
                }
            }
        }
    }

    /**
     * 定期发送心跳，保持浏览器 EventSource 连接并及时发现断线连接。
     */
    @Scheduled(fixedDelayString = "${radar.sse.heartbeat-delay-ms:20000}")
    public void heartbeat() {
        Map<String, String> heartbeat = Map.of("time", LocalDateTime.now().toString());
        for (CopyOnWriteArrayList<LiveSseConnection> userConnections : connections.values()) {
            for (LiveSseConnection connection : userConnections) {
                if (!connection.send("heartbeat", heartbeat)) {
                    remove(connection);
                }
            }
        }
    }

    /**
     * 将事件转换为不带用户关注标识的主播卡片模板。
     *
     * @param event 主播变化事件
     * @return 主播卡片模板
     */
    private LiveAnchorCardVO toCard(LiveAnchorChangedEvent event) {
        LiveSnapshot snapshot = event.getSnapshot();
        LiveAnchorCardVO card = new LiveAnchorCardVO();
        card.setAnchorId(event.getAnchorId());
        card.setPlatform(snapshot.getPlatform().getCode());
        card.setRoomId(snapshot.getRoomId());
        card.setPlatformUid(snapshot.getPlatformUid());
        card.setRoomUrl(event.getRoomUrl());
        card.setAnchorName(snapshot.getAnchorName());
        card.setAvatarUrl(snapshot.getAvatarUrl());
        card.setCoverUrl(snapshot.getCoverUrl());
        card.setLiveTitle(snapshot.getLiveTitle());
        card.setOnlineCount(snapshot.getOnlineCount());
        card.setLiveStatus(snapshot.getLiveStatus().getCode());
        card.setLastCheckTime(event.getLastCheckTime());
        return card;
    }

    /**
     * 复制主播卡片，避免不同用户连接共享可变响应对象。
     *
     * @param source 卡片模板
     * @return 卡片副本
     */
    private LiveAnchorCardVO copyCard(LiveAnchorCardVO source) {
        LiveAnchorCardVO target = new LiveAnchorCardVO();
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
     * 从注册表移除一条连接。
     *
     * @param connection 待移除连接
     */
    private void remove(LiveSseConnection connection) {
        if (connection == null || connection.userId == null) {
            return;
        }
        CopyOnWriteArrayList<LiveSseConnection> userConnections = connections.get(connection.userId);
        if (userConnections == null) {
            return;
        }
        userConnections.remove(connection);
        if (userConnections.isEmpty()) {
            connections.remove(connection.userId, userConnections);
        }
    }

    /**
     * 用户的一条 SSE 连接及其串行发送锁。
     */
    private static final class LiveSseConnection {

        /**
         * 当前用户主键。
         */
        private final Long userId;

        /**
         * SSE 发射器。
         */
        private final SseEmitter emitter;

        private LiveSseConnection(Long userId, SseEmitter emitter) {
            this.userId = userId;
            this.emitter = emitter;
        }

        /**
         * 串行发送一个具名 SSE 事件。
         *
         * @param eventName 事件名称
         * @param data 事件数据
         * @return 是否发送成功
         */
        private synchronized boolean send(String eventName, Object data) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .reconnectTime(RECONNECT_DELAY_MS)
                        .data(data));
                return true;
            } catch (IOException | IllegalStateException exception) {
                return false;
            }
        }
    }
}
