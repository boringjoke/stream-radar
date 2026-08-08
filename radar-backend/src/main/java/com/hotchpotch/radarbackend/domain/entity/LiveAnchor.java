package com.hotchpotch.radarbackend.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 直播主播实体。
 */
@Data
@TableName("live_anchor")
public class LiveAnchor {

    /**
     * 主播主键。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 平台标识：BILIBILI、DOUYU、HUYA、DOUYIN。
     */
    @TableField("platform")
    private String platform;

    /**
     * 平台直播间标识。
     */
    @TableField("room_id")
    private String roomId;

    /**
     * 平台主播用户标识。
     */
    @TableField("platform_uid")
    private String platformUid;

    /**
     * 规范化直播间地址。
     */
    @TableField("room_url")
    private String roomUrl;

    /**
     * 主播名称。
     */
    @TableField("anchor_name")
    private String anchorName;

    /**
     * 主播头像地址。
     */
    @TableField("avatar_url")
    private String avatarUrl;

    /**
     * 直播封面地址。
     */
    @TableField("cover_url")
    private String coverUrl;

    /**
     * 当前或最后一次有效直播标题。
     */
    @TableField("live_title")
    private String liveTitle;

    /**
     * 当前观看人数。
     */
    @TableField("online_count")
    private Long onlineCount;

    /**
     * 直播状态：LIVE、OFFLINE、UNKNOWN、ERROR。
     */
    @TableField("live_status")
    private String liveStatus;

    /**
     * 连续采集失败次数。
     */
    @TableField("failure_count")
    private Integer failureCount;

    /**
     * 最近一次采集错误摘要。
     */
    @TableField(value = "error_message", updateStrategy = FieldStrategy.ALWAYS)
    private String errorMessage;

    /**
     * 最后检测时间。
     */
    @TableField("last_check_time")
    private LocalDateTime lastCheckTime;

    /**
     * 最后成功获取数据时间。
     */
    @TableField("last_success_time")
    private LocalDateTime lastSuccessTime;

    /**
     * 状态最近一次变化时间。
     */
    @TableField("status_change_time")
    private LocalDateTime statusChangeTime;

    /**
     * 创建时间。
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间。
     */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
