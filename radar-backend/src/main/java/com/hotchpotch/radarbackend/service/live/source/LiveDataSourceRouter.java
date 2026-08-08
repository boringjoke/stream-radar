package com.hotchpotch.radarbackend.service.live.source;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.hotchpotch.radarbackend.domain.entity.LiveAnchor;
import com.hotchpotch.radarbackend.domain.enums.LivePlatform;
import com.hotchpotch.radarbackend.service.live.url.ResolvedLiveRoom;
import com.hotchpotch.radarbackend.service.live.validation.LiveRoomAvailabilityChecker;
import com.hotchpotch.radarbackend.service.live.validation.LiveRoomCheckResult;
import com.hotchpotch.radarbackend.service.live.validation.LiveRoomCheckStatus;
import org.springframework.stereotype.Service;

/**
 * 统一直播数据源路由器。
 */
@Service
public class LiveDataSourceRouter implements LiveRoomAvailabilityChecker {

    /**
     * 按平台索引的数据源适配器。
     */
    private final Map<LivePlatform, LivePlatformAdapter> adapters;

    /**
     * 创建统一数据源路由器。
     *
     * @param adapterList 平台适配器列表
     */
    public LiveDataSourceRouter(List<LivePlatformAdapter> adapterList) {
        this.adapters = new EnumMap<>(LivePlatform.class);
        if (adapterList != null) {
            for (LivePlatformAdapter adapter : adapterList) {
                for (LivePlatform platform : LivePlatform.values()) {
                    if (adapter.supports(platform)) {
                        this.adapters.put(platform, adapter);
                    }
                }
            }
        }
    }

    @Override
    public boolean supports(ResolvedLiveRoom room) {
        return room != null && adapters.containsKey(room.getPlatform());
    }

    @Override
    public LiveRoomCheckResult check(ResolvedLiveRoom room) {
        if (!supports(room)) {
            return LiveRoomCheckResult.of(LiveRoomCheckStatus.UNKNOWN);
        }
        LiveSourceResult result = adapters.get(room.getPlatform()).resolve(room);
        return toCheckResult(result);
    }

    /**
     * 查询指定平台主播状态，供后续监控应用服务使用。
     *
     * @param platform 直播平台
     * @param anchors 待监控主播
     * @return 数据源查询结果
     */
    public List<LiveSourceResult> queryStatus(LivePlatform platform, List<LiveAnchor> anchors) {
        LivePlatformAdapter adapter = adapters.get(platform);
        return adapter == null ? List.of() : adapter.queryStatus(anchors);
    }

    /**
     * 判断当前是否已经注册指定平台适配器。
     *
     * @param platform 直播平台
     * @return 是否支持该平台
     */
    public boolean supports(LivePlatform platform) {
        return platform != null && adapters.containsKey(platform);
    }

    /**
     * 将统一数据源结果转换为关注保存使用的校验结果。
     *
     * @param result 数据源结果
     * @return 直播间存在性校验结果
     */
    private LiveRoomCheckResult toCheckResult(LiveSourceResult result) {
        if (result == null) {
            return LiveRoomCheckResult.of(LiveRoomCheckStatus.UNKNOWN);
        }
        return switch (result.getStatus()) {
            case AVAILABLE -> LiveRoomCheckResult.available(result.getSnapshot());
            case NOT_FOUND -> LiveRoomCheckResult.notFound();
            case TEMPORARILY_UNAVAILABLE -> LiveRoomCheckResult.of(
                    LiveRoomCheckStatus.TEMPORARILY_UNAVAILABLE,
                    result.getMessage());
            case UNKNOWN -> LiveRoomCheckResult.of(LiveRoomCheckStatus.UNKNOWN, result.getMessage());
        };
    }
}
