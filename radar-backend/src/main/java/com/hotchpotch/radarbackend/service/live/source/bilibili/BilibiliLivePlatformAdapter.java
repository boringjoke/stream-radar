package com.hotchpotch.radarbackend.service.live.source.bilibili;

import java.util.ArrayList;
import java.util.List;

import com.hotchpotch.radarbackend.domain.entity.LiveAnchor;
import com.hotchpotch.radarbackend.domain.enums.LivePlatform;
import com.hotchpotch.radarbackend.service.live.source.LivePlatformAdapter;
import com.hotchpotch.radarbackend.service.live.source.LiveSourceResult;
import com.hotchpotch.radarbackend.service.live.url.ResolvedLiveRoom;
import org.springframework.stereotype.Component;

/**
 * B站直播平台适配器，负责主备数据源路由。
 */
@Component
public class BilibiliLivePlatformAdapter implements LivePlatformAdapter {

    /**
     * B站主数据源 Provider。
     */
    private final BilibiliApiProvider apiProvider;

    /**
     * B站网页备用数据源 Provider。
     */
    private final BilibiliWebProvider webProvider;

    /**
     * 创建B站平台适配器。
     *
     * @param apiProvider B站主数据源 Provider
     * @param webProvider B站备用数据源 Provider
     */
    public BilibiliLivePlatformAdapter(
            BilibiliApiProvider apiProvider,
            BilibiliWebProvider webProvider) {
        this.apiProvider = apiProvider;
        this.webProvider = webProvider;
    }

    @Override
    public boolean supports(LivePlatform platform) {
        return LivePlatform.BILIBILI == platform;
    }

    @Override
    public LiveSourceResult resolve(ResolvedLiveRoom room) {
        LiveSourceResult primaryResult = apiProvider.resolve(room);
        if (primaryResult.isAvailable()) {
            return primaryResult;
        }

        LiveSourceResult backupResult = webProvider.resolve(room);
        return routeFallback(primaryResult, backupResult);
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
                    : LiveSourceResult.unknown("B站主数据源返回结果数量不足");
            if (primaryResult.isAvailable()) {
                results.add(primaryResult);
                continue;
            }

            LiveAnchor anchor = anchors.get(index);
            if (anchor == null) {
                results.add(LiveSourceResult.unknown("B站监控对象为空"));
                continue;
            }
            ResolvedLiveRoom room = new ResolvedLiveRoom(
                    LivePlatform.BILIBILI,
                    anchor.getRoomId(),
                    anchor.getRoomUrl());
            results.add(routeFallback(primaryResult, webProvider.resolve(room)));
        }
        return results;
    }

    /**
     * 按主备结果执行降级路由。
     *
     * <p>只有主备都明确返回 NOT_FOUND 时，才允许向上游报告房间不存在；
     * 任一方异常、缺失或无法解析时都不能误判为不存在。</p>
     *
     * @param primaryResult 主数据源结果
     * @param backupResult 备用数据源结果
     * @return 路由后的结果
     */
    private LiveSourceResult routeFallback(
            LiveSourceResult primaryResult,
            LiveSourceResult backupResult) {
        if (backupResult == null) {
            return LiveSourceResult.unknown("B站备用数据源未返回结果");
        }
        if (backupResult.isAvailable()) {
            return backupResult;
        }
        if (primaryResult != null
                && primaryResult.getStatus() == com.hotchpotch.radarbackend.service.live.source.LiveSourceStatus.NOT_FOUND
                && backupResult.getStatus() == com.hotchpotch.radarbackend.service.live.source.LiveSourceStatus.NOT_FOUND) {
            return LiveSourceResult.notFound();
        }
        if ((primaryResult != null
                && primaryResult.getStatus() == com.hotchpotch.radarbackend.service.live.source.LiveSourceStatus.TEMPORARILY_UNAVAILABLE)
                || backupResult.getStatus() == com.hotchpotch.radarbackend.service.live.source.LiveSourceStatus.TEMPORARILY_UNAVAILABLE) {
            return LiveSourceResult.temporarilyUnavailable("B站主备数据源均无法可靠返回结果");
        }
        return LiveSourceResult.unknown("B站主备数据源结果无法确认");
    }
}
