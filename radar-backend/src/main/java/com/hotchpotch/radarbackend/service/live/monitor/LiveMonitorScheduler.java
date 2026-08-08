package com.hotchpotch.radarbackend.service.live.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Spring 单实例定时监控触发器。
 */
@Component
public class LiveMonitorScheduler {

    /**
     * 日志记录器。
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(LiveMonitorScheduler.class);

    /**
     * 监控业务服务。
     */
    private final LiveMonitorService liveMonitorService;

    /**
     * 创建定时监控触发器。
     *
     * @param liveMonitorService 监控业务服务
     */
    public LiveMonitorScheduler(LiveMonitorService liveMonitorService) {
        this.liveMonitorService = liveMonitorService;
    }

    /**
     * 按固定间隔执行一次监控。
     */
    @Scheduled(
            fixedDelayString = "${radar.monitor.fixed-delay-ms:60000}",
            initialDelayString = "${radar.monitor.initial-delay-ms:10000}")
    public void monitor() {
        try {
            liveMonitorService.monitorOnce();
        } catch (RuntimeException exception) {
            // 单次数据源或数据库异常不能终止后续调度。
            LOGGER.warn("直播监控任务执行失败", exception);
        }
    }
}
