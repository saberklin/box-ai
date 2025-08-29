package com.boxai.controller;

import com.boxai.auth.JwtAuthFilter;
import com.boxai.common.web.ApiResponse;
import com.boxai.domain.dto.user.UserProfileDTO;
import com.boxai.domain.dto.wechat.LoginResponse;
import com.boxai.domain.dto.wechat.WechatLoginRequest;
import com.boxai.service.WechatAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;


import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户认证控制器
 * 提供微信登录和用户信息接口
 */
@RestController
@RequestMapping("/api/auth")
@Validated
@RequiredArgsConstructor
@Tag(name = "用户认证", description = "用户登录认证相关API")
public class AuthController {
    private final WechatAuthService wechatAuthService;

    /**
     * 微信静默登录
     * @param req 微信登录请求（包含jsCode、昵称、头像等）
     * @return 登录响应（包含JWT令牌和用户ID）
     */
    @PostMapping("/wechat/login")
    @Operation(summary = "微信静默登录", 
               description = "通过微信小程序的jsCode进行用户认证，返回JWT令牌")

    public ApiResponse<LoginResponse> wechatLogin(
            @Parameter(description = "微信登录请求参数", required = true)
            @RequestBody WechatLoginRequest req) {
        return ApiResponse.ok(wechatAuthService.loginSilent(req));
    }

    /**
     * 获取当前用户信息
     * @return 用户详细信息（昵称、头像、创建时间等）
     */
    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息", 
               description = "根据JWT令牌获取当前登录用户的详细信息")

    public ApiResponse<UserProfileDTO> me() {
        Long userId = JwtAuthFilter.CURRENT_USER.get();
        return ApiResponse.ok(wechatAuthService.getCurrentUserProfile(userId));
    }
}


