package com.hotchpotch.radarbackend.service.live.source.douyin;

import java.util.ArrayList;
import java.util.List;

import com.hotchpotch.radarbackend.domain.entity.LiveAnchor;
import com.hotchpotch.radarbackend.domain.enums.LivePlatform;
import com.hotchpotch.radarbackend.service.live.source.LivePlatformAdapter;
import com.hotchpotch.radarbackend.service.live.source.LiveSourceResult;
import com.hotchpotch.radarbackend.service.live.source.LiveSourceStatus;
import com.hotchpotch.radarbackend.service.live.url.ResolvedLiveRoom;
import org.springframework.stereotype.Component;

/**
 * 抖音直播平台适配器，负责主备数据源路由。
 */
@Component
public class DouyinLivePlatformAdapter implements LivePlatformAdapter {

    /**
     * 抖音 JSON 主数据源 Provider。
     */
    private final DouyinApiProvider apiProvider;

    /**
     * 抖音页面备用数据源 Provider。
     */
    private final DouyinWebProvider webProvider;

    /**
     * 创建抖音平台适配器。
     *
     * @param apiProvider 抖音 JSON 主数据源 Provider
     * @param webProvider 抖音页面备用数据源 Provider
     */
    public DouyinLivePlatformAdapter(
            DouyinApiProvider apiProvider,
            DouyinWebProvider webProvider) {
        this.apiProvider = apiProvider;
        this.webProvider = webProvider;
    }

    @Override
    public boolean supports(LivePlatform platform) {
        return LivePlatform.DOUYIN == platform;
    }

    @Override
    public LiveSourceResult resolve(ResolvedLiveRoom room) {
        LiveSourceResult primaryResult = apiProvider.resolve(room);
        if (primaryResult != null && primaryResult.isAvailable()) {
            return primaryResult;
        }
        return routeFallback(primaryResult, webProvider.resolve(room));
    }

    @Override
    public List<LiveSourceResult> queryStatus(List<LiveAnchor> anchors) {
        if (anchors == null || anchors.isEmpty()) {
            return List.of();
        }

        List<LiveSourceResult> primaryResults = apiProvider.queryStatus(anchors);
        List<LiveSourceResult> results = new ArrayList<>(anchors.size());
        for (int index = 0; index < anchors.size(); index++) {
            LiveSourceResult primaryResult = index < primaryResults.size()
                    ? primaryResults.get(index)
                    : LiveSourceResult.unknown("抖音主数据源返回结果数量不足");
            if (primaryResult != null && primaryResult.isAvailable()) {
                results.add(primaryResult);
                continue;
            }

            LiveAnchor anchor = anchors.get(index);
            if (anchor == null || isBlank(anchor.getRoomId())) {
                results.add(LiveSourceResult.unknown("抖音监控对象为空"));
                continue;
            }
            ResolvedLiveRoom room = new ResolvedLiveRoom(
                    LivePlatform.DOUYIN,
                    anchor.getRoomId(),
                    anchor.getRoomUrl());
            results.add(routeFallback(primaryResult, webProvider.resolve(room)));
        }
        return results;
    }

    /**
     * 按主备结果执行降级路由。
     *
     * <p>只有主备数据源都明确返回 NOT_FOUND 时，才允许向上游报告房间不存在；
     * 任一方异常、缺失或无法解析时均不能误判为不存在。</p>
     *
     * @param primaryResult 主数据源结果
     * @param backupResult 备用数据源结果
     * @return 路由后的结果
     */
    private LiveSourceResult routeFallback(
            LiveSourceResult primaryResult,
            LiveSourceResult backupResult) {
        if (backupResult == null) {
            return LiveSourceResult.unknown("抖音备用数据源未返回结果");
        }
        if (backupResult.isAvailable()) {
            return backupResult;
        }
        if (primaryResult != null
                && primaryResult.getStatus() == LiveSourceStatus.NOT_FOUND
                && backupResult.getStatus() == LiveSourceStatus.NOT_FOUND) {
            return LiveSourceResult.notFound();
        }
        if ((primaryResult != null
                && primaryResult.getStatus() == LiveSourceStatus.TEMPORARILY_UNAVAILABLE)
                || backupResult.getStatus() == LiveSourceStatus.TEMPORARILY_UNAVAILABLE) {
            return LiveSourceResult.temporarilyUnavailable("抖音主备数据源均暂时不可用");
        }
        return LiveSourceResult.unknown("抖音主备数据源结果无法确认");
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
