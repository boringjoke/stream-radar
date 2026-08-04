package com.hotchpotch.radarbackend.vo.auth;

/**
 * 当前登录用户的最小会话摘要。
 */
public class SessionUserVO {

    /**
     * 用户主键。
     */
    private final Long id;

    /**
     * 用户名。
     */
    private final String username;

    /**
     * 用户昵称。
     */
    private final String nickname;

    /**
     * 项目内头像静态资源路径，没有头像时为 null。
     */
    private final String avatarPath;

    /**
     * 账号角色：USER 普通用户，ADMIN 管理员。
     */
    private final String role;

    /**
     * 创建当前登录用户会话摘要。
     *
     * @param id 用户主键
     * @param username 用户名
     * @param nickname 用户昵称
     * @param avatarPath 项目内头像静态资源路径
     * @param role 账号角色
     */
    public SessionUserVO(
            Long id,
            String username,
            String nickname,
            String avatarPath,
            String role) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.avatarPath = avatarPath;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getNickname() {
        return nickname;
    }

    public String getAvatarPath() {
        return avatarPath;
    }

    /**
     * 获取账号角色。
     *
     * @return 账号角色
     */
    public String getRole() {
        return role;
    }
}
