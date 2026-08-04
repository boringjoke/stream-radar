package com.hotchpotch.radarbackend.vo.live;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 用户首页主播卡片响应对象。
 */
@Data
public class LiveAnchorCardVO {

    /**
     * 当前用户的关注关系主键。
     */
    private Long followId;

    /**
     * 主播主键。
     */
    private Long anchorId;

    /**
     * 平台标识：BILIBILI、DOUYU、HUYA、DOUYIN。
     */
    private String platform;

    /**
     * 平台直播间标识。
     */
    private String roomId;

    /**
     * 平台主播用户标识，当前阶段可能为空。
     */
    private String platformUid;

    /**
     * 规范化直播间地址。
     */
    private String roomUrl;

    /**
     * 主播名称，当前阶段可能为空。
     */
    private String anchorName;

    /**
     * 主播头像地址，当前阶段可能为空。
     */
    private String avatarUrl;

    /**
     * 直播封面地址，当前阶段可能为空。
     */
    private String coverUrl;

    /**
     * 当前或最后一次有效直播标题，当前阶段可能为空。
     */
    private String liveTitle;

    /**
     * 当前观看人数，当前阶段可能为空。
     */
    private Long onlineCount;

    /**
     * 统一直播状态：LIVE、OFFLINE、UNKNOWN、ERROR。
     */
    private String liveStatus;

    /**
     * 最后检测时间，当前阶段可能为空。
     */
    private LocalDateTime lastCheckTime;
}
