package com.hotchpotch.radarbackend.service.live.source;

import java.util.List;

import com.hotchpotch.radarbackend.domain.entity.LiveAnchor;
import com.hotchpotch.radarbackend.domain.enums.LivePlatform;
import com.hotchpotch.radarbackend.service.live.url.ResolvedLiveRoom;

/**
 * 统一直播平台适配器接口。
 */
public interface LivePlatformAdapter {

    /**
     * 判断适配器是否负责指定平台。
     *
     * @param platform 直播平台
     * @return 是否支持该平台
     */
    boolean supports(LivePlatform platform);

    /**
     * 查询直播间资料和当前状态。
     *
     * @param room URL 解析后的直播间身份
     * @return 数据源查询结果
     */
    LiveSourceResult resolve(ResolvedLiveRoom room);

    /**
     * 批量查询已保存主播的状态。
     *
     * @param anchors 待查询主播列表
     * @return 与输入主播顺序对应的查询结果
     */
    List<LiveSourceResult> queryStatus(List<LiveAnchor> anchors);
}
