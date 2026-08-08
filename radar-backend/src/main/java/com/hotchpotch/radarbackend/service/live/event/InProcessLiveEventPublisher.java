package com.hotchpotch.radarbackend.service.live.event;

import java.util.List;

import org.springframework.stereotype.Component;

/**
 * 单实例进程内直播事件发布器。
 */
@Component
public class InProcessLiveEventPublisher implements LiveEventPublisher {

    /**
     * 当前进程内的事件订阅器。
     */
    private final List<LiveEventSubscriber> subscribers;

    /**
     * 创建进程内事件发布器。
     *
     * @param subscribers 事件订阅器列表
     */
    public InProcessLiveEventPublisher(List<LiveEventSubscriber> subscribers) {
        this.subscribers = subscribers == null ? List.of() : List.copyOf(subscribers);
    }

    @Override
    public void publish(LiveAnchorChangedEvent event) {
        if (event == null) {
            return;
        }
        for (LiveEventSubscriber subscriber : subscribers) {
            subscriber.onLiveAnchorChanged(event);
        }
    }
}
