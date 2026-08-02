package com.hotchpotch.radarbackend.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户关注主播关系实体。
 */
@Data
@TableName("user_follow_anchor")
public class UserFollowAnchor {

    /**
     * 关注关系主键。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户主键，由业务代码维护关联关系。
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 主播主键，由业务代码维护关联关系。
     */
    @TableField("anchor_id")
    private Long anchorId;

    /**
     * 关注创建时间。
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 关注更新时间。
     */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
