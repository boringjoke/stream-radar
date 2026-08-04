package com.hotchpotch.radarbackend.vo.live;

import java.util.List;

/**
 * 当前用户直播首页响应对象。
 */
public class LiveHomeVO {

    /**
     * 当前用户关注主播总数。
     */
    private final long totalCount;

    /**
     * 当前状态为直播中的主播数量。
     */
    private final long liveCount;

    /**
     * 当前用户关注主播卡片列表。
     */
    private final List<LiveAnchorCardVO> anchors;

    /**
     * 创建直播首页响应对象。
     *
     * @param totalCount 关注主播总数
     * @param liveCount 直播中主播数量
     * @param anchors 主播卡片列表
     */
    public LiveHomeVO(long totalCount, long liveCount, List<LiveAnchorCardVO> anchors) {
        this.totalCount = totalCount;
        this.liveCount = liveCount;
        this.anchors = anchors == null ? List.of() : anchors;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public long getLiveCount() {
        return liveCount;
    }

    public List<LiveAnchorCardVO> getAnchors() {
        return anchors;
    }
}
