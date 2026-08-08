package com.hotchpotch.radarbackend.service.live.event;

/**
 * 直播变化事件发布器抽象。
 *
 * <p>当前实现为单实例进程内发布；未来扩展多实例部署时可替换为 Redis Pub/Sub，
 * 不将 SSE 连接对象写入 Redis。</p>
 */
public interface LiveEventPublisher {

    /**
     * 发布已完成数据库提交的主播变化事件。
     *
     * @param event 主播变化事件
     */
    void publish(LiveAnchorChangedEvent event);
}
