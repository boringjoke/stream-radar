package com.hotchpotch.radarbackend.service.user;

import java.util.List;
import java.util.Locale;

import com.hotchpotch.radarbackend.common.exception.BusinessException;
import com.hotchpotch.radarbackend.common.exception.ErrorCode;
import com.hotchpotch.radarbackend.domain.entity.SysUser;
import com.hotchpotch.radarbackend.domain.repository.SysUserRepository;
import com.hotchpotch.radarbackend.request.user.ProfileUpdateRequest;
import com.hotchpotch.radarbackend.security.RadarUserPrincipal;
import com.hotchpotch.radarbackend.vo.user.AvatarOptionVO;
import com.hotchpotch.radarbackend.vo.user.UserProfileVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * 用户中心业务服务。
 */
@Service
public class UserService {

    /**
     * 项目内置头像白名单。
     */
    private static final List<AvatarOptionVO> AVATAR_OPTIONS = List.of(
            new AvatarOptionVO("/avatars/avatar-01.svg", "头像 01"),
            new AvatarOptionVO("/avatars/avatar-02.svg", "头像 02"),
            new AvatarOptionVO("/avatars/avatar-03.svg", "头像 03"),
            new AvatarOptionVO("/avatars/avatar-04.svg", "头像 04"),
            new AvatarOptionVO("/avatars/avatar-05.svg", "头像 05"),
            new AvatarOptionVO("/avatars/avatar-06.svg", "头像 06"),
            new AvatarOptionVO("/avatars/avatar-07.svg", "头像 07"),
            new AvatarOptionVO("/avatars/avatar-08.svg", "头像 08"));

    /**
     * 系统用户仓库。
     */
    private final SysUserRepository userRepository;

    /**
     * 创建用户中心业务服务。
     *
     * @param userRepository 系统用户仓库
     */
    public UserService(SysUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 查询当前登录用户资料。
     *
     * @param authentication 当前认证对象
     * @return 当前用户资料
     */
    public UserProfileVO getProfile(Authentication authentication) {
        return toProfile(getCurrentUser(authentication));
    }

    /**
     * 查询项目内置头像选项。
     *
     * @return 头像选项列表
     */
    public List<AvatarOptionVO> getAvatarOptions() {
        return AVATAR_OPTIONS;
    }

    /**
     * 更新当前登录用户资料。
     *
     * @param authentication 当前认证对象
     * @param request 资料更新请求
     * @return 更新后的用户资料
     */
    public UserProfileVO updateProfile(
            Authentication authentication,
            ProfileUpdateRequest request) {
        SysUser user = getCurrentUser(authentication);
        String nickname = normalizeNickname(request.getNickname());
        String email = normalizeEmail(request.getEmail());
        String avatarPath = normalizeAvatarPath(request.getAvatarPath());

        if (email != null) {
            userRepository.findByEmail(email)
                    .filter(existingUser -> !existingUser.getId().equals(user.getId()))
                    .ifPresent(existingUser -> {
                        throw new BusinessException(ErrorCode.BUSINESS_ERROR, "邮箱已存在");
                    });
        }

        user.setNickname(nickname);
        user.setEmail(email);
        user.setAvatarPath(avatarPath);
        try {
            userRepository.updateById(user);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "邮箱已存在", exception);
        }
        return toProfile(user);
    }

    /**
     * 根据当前认证主体查询用户实体。
     *
     * @param authentication 当前认证对象
     * @return 当前启用的用户实体
     */
    private SysUser getCurrentUser(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof RadarUserPrincipal principal)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return userRepository.findById(principal.getUserId())
                .filter(user -> Integer.valueOf(1).equals(user.getStatus()))
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    /**
     * 规范化用户昵称。
     *
     * @param nickname 原始昵称
     * @return 去除首尾空白后的昵称
     */
    private String normalizeNickname(String nickname) {
        String normalizedNickname = nickname.trim();
        if (normalizedNickname.isBlank()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "昵称不能为空");
        }
        return normalizedNickname;
    }

    /**
     * 规范化可选邮箱。
     *
     * @param email 原始邮箱
     * @return 规范化后的邮箱，空值时返回 null
     */
    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 校验并规范化头像路径。
     *
     * @param avatarPath 原始头像路径
     * @return 白名单中的头像路径，空值时返回 null
     */
    private String normalizeAvatarPath(String avatarPath) {
        if (avatarPath == null || avatarPath.isBlank()) {
            return null;
        }

        String normalizedAvatarPath = avatarPath.trim();
        boolean allowed = AVATAR_OPTIONS.stream()
                .anyMatch(option -> option.getPath().equals(normalizedAvatarPath));
        if (!allowed) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "头像必须选择项目内置选项");
        }
        return normalizedAvatarPath;
    }

    /**
     * 将系统用户实体转换为用户资料响应对象。
     *
     * @param user 系统用户实体
     * @return 用户资料响应对象
     */
    private UserProfileVO toProfile(SysUser user) {
        return new UserProfileVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getAvatarPath());
    }
}
