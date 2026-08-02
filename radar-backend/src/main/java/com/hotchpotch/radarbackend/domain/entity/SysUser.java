package com.hotchpotch.radarbackend.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 系统用户实体。
 */
@Data
@TableName("sys_user")
public class SysUser {

    /**
     * 用户主键。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户名，统一按小写保存且不可修改。
     */
    @TableField("username")
    private String username;

    /**
     * 密码摘要，不保存明文密码。
     */
    @TableField("password_hash")
    private String passwordHash;

    /**
     * 用户邮箱，可为空。
     */
    @TableField("email")
    private String email;

    /**
     * 用户昵称。
     */
    @TableField("nickname")
    private String nickname;

    /**
     * 项目内头像静态资源相对路径或资源标识，不存外部 URL。
     */
    @TableField("avatar_path")
    private String avatarPath;

    /**
     * 账号状态：1 启用，0 停用。
     */
    @TableField("status")
    private Integer status;

    /**
     * 最后登录时间。
     */
    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

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
