package com.hotchpotch.radarbackend.security;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hotchpotch.radarbackend.domain.entity.SysUser;
import com.hotchpotch.radarbackend.domain.enums.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 保存在认证上下文中的最小用户主体。
 *
 * <p>密码摘要仅在认证查询阶段暂存，并标记为 transient，避免写入 Redis Session。</p>
 */
public final class RadarUserPrincipal implements UserDetails, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户主键。
     */
    private final Long userId;

    /**
     * 用户名。
     */
    private final String username;

    /**
     * 账号角色。
     */
    private final String role;

    /**
     * 密码摘要，仅用于当前认证过程，不写入 Session。
     */
    private final transient String passwordHash;

    /**
     * 认证时的账号启用状态。
     */
    private final boolean enabled;

    private RadarUserPrincipal(
            Long userId,
            String username,
            String role,
            String passwordHash,
            boolean enabled) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
    }

    /**
     * 从系统用户实体创建认证主体。
     *
     * @param user 系统用户实体
     * @return 最小认证主体
     */
    public static RadarUserPrincipal from(SysUser user) {
        return new RadarUserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getPasswordHash(),
                Integer.valueOf(1).equals(user.getStatus()));
    }

    /**
     * 获取用户主键。
     *
     * @return 用户主键
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 获取账号角色。
     *
     * @return 账号角色
     */
    public String getRole() {
        return role;
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public List<? extends GrantedAuthority> getAuthorities() {
        String authority = UserRole.ADMIN.getCode().equals(role)
                ? "ROLE_ADMIN"
                : "ROLE_USER";
        return List.of(new SimpleGrantedAuthority(authority));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
