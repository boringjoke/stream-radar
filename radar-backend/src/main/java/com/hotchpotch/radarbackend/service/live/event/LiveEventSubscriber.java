package com.hotchpotch.radarbackend.service.live.event;

/**
 * 直播变化事件订阅器。
 */
public interface LiveEventSubscriber {

    /**
     * 消费一个主播变化事件。
     *
     * @param event 主播变化事件
     */
    void onLiveAnchorChanged(LiveAnchorChangedEvent event);
}
