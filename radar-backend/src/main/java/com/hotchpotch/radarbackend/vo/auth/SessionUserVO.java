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

    public SessionUserVO(Long id, String username, String nickname, String avatarPath) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.avatarPath = avatarPath;
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
}
