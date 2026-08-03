package com.hotchpotch.radarbackend.vo.user;

/**
 * 当前登录用户资料。
 */
public class UserProfileVO {

    /**
     * 用户主键。
     */
    private final Long id;

    /**
     * 用户名，只读。
     */
    private final String username;

    /**
     * 用户昵称。
     */
    private final String nickname;

    /**
     * 用户邮箱，没有填写时为 null。
     */
    private final String email;

    /**
     * 项目内头像静态资源路径，没有选择时为 null。
     */
    private final String avatarPath;

    public UserProfileVO(Long id, String username, String nickname, String email, String avatarPath) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.email = email;
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

    public String getEmail() {
        return email;
    }

    public String getAvatarPath() {
        return avatarPath;
    }
}
