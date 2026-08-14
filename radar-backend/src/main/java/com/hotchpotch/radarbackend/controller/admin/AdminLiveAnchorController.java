package com.hotchpotch.radarbackend.controller.admin;

import com.hotchpotch.radarbackend.common.response.ApiResponse;
import com.hotchpotch.radarbackend.request.admin.AdminLiveAnchorPageRequest;
import com.hotchpotch.radarbackend.service.admin.AdminLiveAnchorService;
import com.hotchpotch.radarbackend.vo.admin.AdminLiveAnchorVO;
import com.hotchpotch.radarbackend.vo.common.PageVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
     * 管理中心主播接口。
 */
@RestController
@RequestMapping("/api/admin/liveAnchor")
public class AdminLiveAnchorController {

    /**
     * 管理中心主播业务服务。
     */
    private final AdminLiveAnchorService adminLiveAnchorService;

    /**
     * 创建管理中心主播控制器。
     *
     * @param adminLiveAnchorService 管理中心主播业务服务
     */
    public AdminLiveAnchorController(AdminLiveAnchorService adminLiveAnchorService) {
        this.adminLiveAnchorService = adminLiveAnchorService;
    }

    /**
     * 分页查询全部主播及当前启用普通用户的关注人数。
     *
     * @param request 分页查询请求
     * @return 管理中心主播分页结果
     */
    @PostMapping("/page")
    public ApiResponse<PageVO<AdminLiveAnchorVO>> page(
            @Valid @RequestBody AdminLiveAnchorPageRequest request) {
        return ApiResponse.success(adminLiveAnchorService.page(request));
    }
}
