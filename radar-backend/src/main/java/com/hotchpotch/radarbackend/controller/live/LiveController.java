package com.hotchpotch.radarbackend.controller.live;

import com.hotchpotch.radarbackend.common.response.ApiResponse;
import com.hotchpotch.radarbackend.request.live.LiveFollowRequest;
import com.hotchpotch.radarbackend.request.live.LiveUnfollowBatchRequest;
import com.hotchpotch.radarbackend.request.live.LiveUnfollowRequest;
import com.hotchpotch.radarbackend.service.live.LiveService;
import com.hotchpotch.radarbackend.service.live.sse.LiveSseService;
import com.hotchpotch.radarbackend.vo.live.LiveAnchorCardVO;
import com.hotchpotch.radarbackend.vo.live.LiveHomeVO;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 直播首页和关注管理接口。
 */
@RestController
@RequestMapping("/api/live")
public class LiveController {

    /**
     * 直播业务服务。
     */
    private final LiveService liveService;

    /**
     * 直播 SSE 服务。
     */
    private final LiveSseService liveSseService;

    /**
     * 创建直播接口控制器。
     *
     * @param liveService 直播业务服务
     */
    public LiveController(LiveService liveService, LiveSseService liveSseService) {
        this.liveService = liveService;
        this.liveSseService = liveSseService;
    }

    /**
     * 查询当前用户关注主播首页数据。
     *
     * @param authentication 当前认证对象
     * @return 首页主播数据
     */
    @GetMapping("/home")
    public ApiResponse<LiveHomeVO> home(Authentication authentication) {
        return ApiResponse.success(liveService.getHome(authentication));
    }

    /**
     * 建立当前用户的直播状态 SSE 长连接。
     *
     * @param authentication 当前认证对象
     * @return SSE 发射器
     */
    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(Authentication authentication) {
        return liveSseService.connect(authentication);
    }

    /**
     * 关注一个直播间。
     *
     * @param authentication 当前认证对象
     * @param request 关注请求
     * @return 新增关注对应的主播卡片
     */
    @PostMapping("/follow")
    public ApiResponse<LiveAnchorCardVO> follow(
            Authentication authentication,
            @Valid @RequestBody LiveFollowRequest request) {
        return ApiResponse.success(liveService.follow(authentication, request));
    }

    /**
     * 取消一个关注关系。
     *
     * @param authentication 当前认证对象
     * @param request 取消关注请求
     * @return 无业务数据的成功响应
     */
    @PostMapping("/unfollow")
    public ApiResponse<Void> unfollow(
            Authentication authentication,
            @Valid @RequestBody LiveUnfollowRequest request) {
        liveService.unfollow(authentication, request);
        return ApiResponse.success();
    }

    /**
     * 批量取消关注关系。
     *
     * @param authentication 当前认证对象
     * @param request 批量取消关注请求
     * @return 无业务数据的成功响应
     */
    @PostMapping("/unfollow/batch")
    public ApiResponse<Void> unfollowBatch(
            Authentication authentication,
            @Valid @RequestBody LiveUnfollowBatchRequest request) {
        liveService.unfollowBatch(authentication, request);
        return ApiResponse.success();
    }
}
