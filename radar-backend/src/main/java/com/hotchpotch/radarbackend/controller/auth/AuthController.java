package com.hotchpotch.radarbackend.controller.auth;

import com.hotchpotch.radarbackend.common.response.ApiResponse;
import com.hotchpotch.radarbackend.request.auth.LoginRequest;
import com.hotchpotch.radarbackend.request.auth.RegisterRequest;
import com.hotchpotch.radarbackend.service.auth.AuthService;
import com.hotchpotch.radarbackend.vo.auth.AuthSessionVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 注册、登录和 Session 接口。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /**
     * 认证业务服务。
     */
    private final AuthService authService;

    /**
     * 创建认证控制器。
     *
     * @param authService 认证业务服务
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 注册用户并自动登录。
     *
     * @param request 注册请求
     * @param httpRequest 当前 HTTP 请求
     * @param httpResponse 当前 HTTP 响应
     * @return 已建立的认证 Session
     */
    @PostMapping("/register")
    public ApiResponse<AuthSessionVO> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        return ApiResponse.success(authService.register(request, httpRequest, httpResponse));
    }

    /**
     * 登录用户。
     *
     * @param request 登录请求
     * @param httpRequest 当前 HTTP 请求
     * @param httpResponse 当前 HTTP 响应
     * @return 已建立的认证 Session
     */
    @PostMapping("/login")
    public ApiResponse<AuthSessionVO> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        return ApiResponse.success(authService.login(request, httpRequest, httpResponse));
    }

    /**
     * 查询当前登录 Session。
     *
     * @param authentication 当前认证对象
     * @return Session 查询结果
     */
    @GetMapping("/session")
    public ApiResponse<AuthSessionVO> session(Authentication authentication) {
        return ApiResponse.success(authService.getSession(authentication));
    }

    /**
     * 初始化 CSRF Token Cookie。
     *
     * @param httpRequest 当前 HTTP 请求
     * @param httpResponse 当前 HTTP 响应
     * @return 无业务数据的成功响应
     */
    @GetMapping("/csrf")
    public ApiResponse<Void> csrf(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        authService.initializeCsrfToken(httpRequest, httpResponse);
        return ApiResponse.success();
    }
}
