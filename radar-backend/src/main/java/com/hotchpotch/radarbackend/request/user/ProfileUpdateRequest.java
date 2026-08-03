package com.hotchpotch.radarbackend.request.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户资料更新请求。
 */
public class ProfileUpdateRequest {

    /**
     * 用户昵称。
     */
    @NotBlank(message = "昵称不能为空")
    @Size(max = 64, message = "昵称长度不能超过 64 位")
    private String nickname;

    /**
     * 可选邮箱地址，传空值时清空邮箱。
     */
    @Size(max = 255, message = "邮箱长度不能超过 255 位")
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 项目内预置头像路径，传空值时清空头像。
     */
    @Size(max = 512, message = "头像路径长度不能超过 512 位")
    private String avatarPath;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatarPath() {
        return avatarPath;
    }

    public void setAvatarPath(String avatarPath) {
        this.avatarPath = avatarPath;
    }
}
