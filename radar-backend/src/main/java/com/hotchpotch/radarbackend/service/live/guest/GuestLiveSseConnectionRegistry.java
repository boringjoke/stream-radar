package com.hotchpotch.radarbackend.service.live.guest;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.hotchpotch.radarbackend.config.LiveSseProperties;
import com.hotchpotch.radarbackend.vo.live.LiveHomeVO;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 游客首页单实例进程内 SSE 连接注册表。
 *
 * <p>游客连接不绑定用户和关注关系，只接收四个固定真实演示主播的完整快照。</p>
 */
@Component
public class GuestLiveSseConnectionRegistry {

    /**
     * SSE 重连建议间隔，单位毫秒。
     */
    private static final long RECONNECT_DELAY_MS = 3000L;

    /**
     * 当前进程内的游客首页连接。
     */
    private final CopyOnWriteArrayList<GuestLiveSseConnection> connections
            = new CopyOnWriteArrayList<>();

    /**
     * SSE 配置。
     */
    private final LiveSseProperties properties;

    /**
     * 创建游客首页 SSE 注册表。
     *
     * @param properties SSE 配置
     */
    public GuestLiveSseConnectionRegistry(LiveSseProperties properties) {
        this.properties = properties;
    }

    /**
     * 注册游客连接并发送初始完整快照。
     *
     * @param home 游客首页快照
     * @return SSE 发射器
     */
    public SseEmitter connect(LiveHomeVO home) {
        long timeout = Math.max(0L, properties.getEmitterTimeoutMs());
        SseEmitter emitter = new SseEmitter(timeout);
        GuestLiveSseConnection connection = new GuestLiveSseConnection(emitter);
        emitter.onCompletion(() -> remove(connection));
        emitter.onTimeout(() -> remove(connection));
        emitter.onError(error -> remove(connection));
        connections.add(connection);

        if (!connection.send("snapshot", home)) {
            remove(connection);
        }
        return emitter;
    }

    /**
     * 向全部游客连接广播最新完整快照。
     *
     * @param home 游客首页快照
     */
    public void publish(LiveHomeVO home) {
        if (home == null) {
            return;
        }
        for (GuestLiveSseConnection connection : connections) {
            if (!connection.send("snapshot", home)) {
                remove(connection);
            }
        }
    }

    /**
     * 定期发送心跳，保持浏览器 EventSource 连接并发现断线连接。
     */
    @Scheduled(fixedDelayString = "${radar.sse.heartbeat-delay-ms:20000}")
    public void heartbeat() {
        Map<String, String> heartbeat = Map.of("time", LocalDateTime.now().toString());
        for (GuestLiveSseConnection connection : connections) {
            if (!connection.send("heartbeat", heartbeat)) {
                remove(connection);
            }
        }
    }

    /**
     * 移除一条游客连接。
     *
     * @param connection 待移除连接
     */
    private void remove(GuestLiveSseConnection connection) {
        connections.remove(connection);
    }

    /**
     * 游客首页的一条 SSE 连接及其串行发送锁。
     */
    private static final class GuestLiveSseConnection {

        /**
         * SSE 发射器。
         */
        private final SseEmitter emitter;

        private GuestLiveSseConnection(SseEmitter emitter) {
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
