package com.hotchpotch.radarbackend.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 用户注册请求。
 */
public class RegisterRequest {

    /**
     * 用户名，注册后不可修改。
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 32, message = "用户名长度必须为 4～32 位")
    @Pattern(regexp = "[A-Za-z0-9_]+", message = "用户名只能包含英文字母、数字或下划线")
    private String username;

    /**
     * 登录密码。
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 72, message = "密码长度必须为 8～72 位")
    private String password;

    /**
     * 确认密码。
     */
    @NotBlank(message = "确认密码不能为空")
    @Size(min = 8, max = 72, message = "确认密码长度必须为 8～72 位")
    private String confirmPassword;

    /**
     * 可选邮箱地址。
     */
    @Size(max = 255, message = "邮箱长度不能超过 255 位")
    @Email(message = "邮箱格式不正确")
    private String email;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
