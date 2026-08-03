package com.hotchpotch.radarbackend.controller.user;

import java.util.List;

import com.hotchpotch.radarbackend.common.response.ApiResponse;
import com.hotchpotch.radarbackend.request.user.ProfileUpdateRequest;
import com.hotchpotch.radarbackend.service.user.UserService;
import com.hotchpotch.radarbackend.vo.user.AvatarOptionVO;
import com.hotchpotch.radarbackend.vo.user.UserProfileVO;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户中心接口。
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    /**
     * 用户中心业务服务。
     */
    private final UserService userService;

    /**
     * 创建用户中心控制器。
     *
     * @param userService 用户中心业务服务
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 查询当前登录用户资料。
     *
     * @param authentication 当前认证对象
     * @return 当前用户资料
     */
    @GetMapping("/profile")
    public ApiResponse<UserProfileVO> profile(Authentication authentication) {
        return ApiResponse.success(userService.getProfile(authentication));
    }

    /**
     * 查询项目内置头像选项。
     *
     * @return 头像选项列表
     */
    @GetMapping("/avatarOptions")
    public ApiResponse<List<AvatarOptionVO>> avatarOptions() {
        return ApiResponse.success(userService.getAvatarOptions());
    }

    /**
     * 更新当前登录用户资料。
     *
     * @param authentication 当前认证对象
     * @param request 资料更新请求
     * @return 更新后的用户资料
     */
    @PostMapping("/profile/update")
    public ApiResponse<UserProfileVO> updateProfile(
            Authentication authentication,
            @Valid @RequestBody ProfileUpdateRequest request) {
        return ApiResponse.success(userService.updateProfile(authentication, request));
    }
}
