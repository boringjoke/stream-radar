package com.hotchpotch.radarbackend.security;

import java.util.Locale;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRepository;

/**
 * StreamRadar Spring Security 基础配置。
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(RadarSecurityProperties.class)
public class SecurityConfig {

    /**
     * CSRF Cookie 固定名称。
     */
    private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";

    /**
     * CSRF 请求头固定名称。
     */
    private static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";

    /**
     * 创建密码编码器。
     *
     * @return BCrypt 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 创建认证管理器，供后续 JSON 登录接口使用。
     *
     * @param authenticationConfiguration Spring Security 认证配置
     * @return 认证管理器
     * @throws Exception 获取认证管理器失败
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * 创建兼容请求属性和 Redis Session 的安全上下文仓库。
     *
     * @return 安全上下文仓库
     */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new DelegatingSecurityContextRepository(
                new RequestAttributeSecurityContextRepository(),
                new HttpSessionSecurityContextRepository());
    }

    /**
     * 创建登录时使用的 Session Fixation 防护策略。
     *
     * @return Session Fixation 防护策略
     */
    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new ChangeSessionIdAuthenticationStrategy();
    }

    /**
     * 创建 Cookie Token 方式的 CSRF 仓库。
     *
     * @param securityProperties 安全配置
     * @return CSRF Token 仓库
     */
    @Bean
    public CsrfTokenRepository csrfTokenRepository(RadarSecurityProperties securityProperties) {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieName(CSRF_COOKIE_NAME);
        repository.setHeaderName(CSRF_HEADER_NAME);
        repository.setCookiePath("/");
        repository.setCookieCustomizer(cookie -> {
            cookie.httpOnly(false)
                    .secure(securityProperties.getCsrf().isCookieSecure());
            String sameSite = securityProperties.getCsrf().getCookieSameSite();
            if (sameSite != null && !sameSite.isBlank()) {
                cookie.sameSite(sameSite.toLowerCase(Locale.ROOT));
            }
        });
        return repository;
    }

    /**
     * 创建安全过滤器链。
     *
     * @param http HTTP 安全配置构建器
     * @param csrfTokenRepository CSRF Token 仓库
     * @param securityContextRepository 安全上下文仓库
     * @param authenticationEntryPoint 未认证处理器
     * @param accessDeniedHandler 无权限处理器
     * @param logoutSuccessHandler 退出登录成功处理器
     * @return 安全过滤器链
     * @throws Exception 安全配置失败
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CsrfTokenRepository csrfTokenRepository,
            SecurityContextRepository securityContextRepository,
            ApiAuthenticationEntryPoint authenticationEntryPoint,
            ApiAccessDeniedHandler accessDeniedHandler,
            ApiLogoutSuccessHandler logoutSuccessHandler) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .securityContext(securityContext -> securityContext
                        .securityContextRepository(securityContextRepository))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(sessionFixation -> sessionFixation.changeSessionId()))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/logout",
                                "/api/auth/session",
                                "/api/auth/csrf")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/user/avatarOptions")
                        .permitAll()
                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/api/user/**")
                        .authenticated()
                        .anyRequest()
                        .permitAll())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .requestCache(requestCache -> requestCache.disable())
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .clearAuthentication(true)
                        .invalidateHttpSession(true)
                        .logoutSuccessHandler(logoutSuccessHandler));
        return http.build();
    }
}
