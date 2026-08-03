package com.hotchpotch.radarbackend.service.auth;

import java.time.LocalDateTime;
import java.util.Locale;

import com.hotchpotch.radarbackend.common.exception.BusinessException;
import com.hotchpotch.radarbackend.common.exception.ErrorCode;
import com.hotchpotch.radarbackend.domain.entity.SysUser;
import com.hotchpotch.radarbackend.domain.repository.SysUserRepository;
import com.hotchpotch.radarbackend.request.auth.LoginRequest;
import com.hotchpotch.radarbackend.request.auth.RegisterRequest;
import com.hotchpotch.radarbackend.security.RadarUserPrincipal;
import com.hotchpotch.radarbackend.vo.auth.AuthSessionVO;
import com.hotchpotch.radarbackend.vo.auth.SessionUserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DeferredCsrfToken;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证业务服务。
 */
@Service
public class AuthService {

    /**
     * 系统用户仓库。
     */
    private final SysUserRepository userRepository;

    /**
     * 密码编码器。
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * Spring Security 认证管理器。
     */
    private final AuthenticationManager authenticationManager;

    /**
     * Session Fixation 防护策略。
     */
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;

    /**
     * 安全上下文持久化仓库。
     */
    private final SecurityContextRepository securityContextRepository;

    /**
     * CSRF Token 仓库。
     */
    private final CsrfTokenRepository csrfTokenRepository;

    /**
     * 创建认证业务服务。
     *
     * @param userRepository 系统用户仓库
     * @param passwordEncoder 密码编码器
     * @param authenticationManager 认证管理器
     * @param sessionAuthenticationStrategy Session Fixation 防护策略
     * @param securityContextRepository 安全上下文持久化仓库
     * @param csrfTokenRepository CSRF Token 仓库
     */
    public AuthService(
            SysUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            SecurityContextRepository securityContextRepository,
            CsrfTokenRepository csrfTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.securityContextRepository = securityContextRepository;
        this.csrfTokenRepository = csrfTokenRepository;
    }

    /**
     * 注册用户并自动建立登录 Session。
     *
     * @param request 注册请求
     * @param httpRequest 当前 HTTP 请求
     * @param httpResponse 当前 HTTP 响应
     * @return 已建立的认证 Session
     */
    public AuthSessionVO register(
            RegisterRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "两次输入的密码不一致");
        }

        String username = normalizeUsername(request.getUsername());
        String email = normalizeEmail(request.getEmail());
        if (userRepository.findByUsername(username).isPresent()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "用户名已存在");
        }
        if (email != null && userRepository.findByEmail(email).isPresent()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "邮箱已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEmail(email);
        user.setNickname(username);
        user.setStatus(1);

        try {
            userRepository.insert(user);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "用户名或邮箱已存在", exception);
        }

        return authenticateAndCreateSession(username, request.getPassword(), httpRequest, httpResponse);
    }

    /**
     * 校验登录信息并建立 Redis Session。
     *
     * @param request 登录请求
     * @param httpRequest 当前 HTTP 请求
     * @param httpResponse 当前 HTTP 响应
     * @return 已建立的认证 Session
     */
    public AuthSessionVO login(
            LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        return authenticateAndCreateSession(
                normalizeUsername(request.getUsername()),
                request.getPassword(),
                httpRequest,
                httpResponse);
    }

    /**
     * 查询当前请求对应的登录 Session。
     *
     * @param authentication 当前认证对象
     * @return Session 查询结果
     */
    public AuthSessionVO getSession(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof RadarUserPrincipal principal)) {
            return AuthSessionVO.unauthenticated();
        }

        return userRepository.findById(principal.getUserId())
                .filter(user -> Integer.valueOf(1).equals(user.getStatus()))
                .map(this::toSession)
                .orElseGet(AuthSessionVO::unauthenticated);
    }

    /**
     * 触发 CSRF Token 生成，使 XSRF-TOKEN Cookie 写入当前响应。
     *
     * @param httpRequest 当前 HTTP 请求
     * @param httpResponse 当前 HTTP 响应
     */
    public void initializeCsrfToken(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        DeferredCsrfToken deferredToken = csrfTokenRepository.loadDeferredToken(httpRequest, httpResponse);
        deferredToken.get().getToken();
    }

    /**
     * 完成认证、更新登录时间并保存安全上下文。
     *
     * @param username 规范化后的用户名
     * @param password 登录密码
     * @param httpRequest 当前 HTTP 请求
     * @param httpResponse 当前 HTTP 响应
     * @return 已建立的认证 Session
     */
    private AuthSessionVO authenticateAndCreateSession(
            String username,
            String password,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(username, password));
        } catch (AuthenticationException exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        } catch (RuntimeException exception) {
            throw new AuthenticationServiceException("认证服务暂时不可用", exception);
        }

        if (!(authentication.getPrincipal() instanceof RadarUserPrincipal principal)) {
            throw new AuthenticationServiceException("认证主体类型不受支持");
        }

        SysUser user = userRepository.findById(principal.getUserId())
                .filter(item -> Integer.valueOf(1).equals(item.getStatus()))
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误"));
        user.setLastLoginTime(LocalDateTime.now());
        userRepository.updateById(user);

        sessionAuthenticationStrategy.onAuthentication(authentication, httpRequest, httpResponse);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        securityContextRepository.saveContext(securityContext, httpRequest, httpResponse);

        return toSession(user);
    }

    /**
     * 规范化用户名。
     *
     * @param username 原始用户名
     * @return 去除首尾空白并转换为小写后的用户名
     */
    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 规范化可选邮箱。
     *
     * @param email 原始邮箱
     * @return 去除首尾空白并转换为小写后的邮箱，空值时返回 null
     */
    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 将系统用户实体转换为会话响应对象。
     *
     * @param user 系统用户实体
     * @return 会话响应对象
     */
    private AuthSessionVO toSession(SysUser user) {
        return AuthSessionVO.authenticated(new SessionUserVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatarPath()));
    }
}
