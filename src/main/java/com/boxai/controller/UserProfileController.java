package com.boxai.controller;

import com.boxai.auth.JwtAuthFilter;
import com.boxai.common.web.ApiResponse;
import com.boxai.domain.dto.user.UserProfileAnalysisDTO;
import com.boxai.domain.dto.request.BehaviorRecordRequest;
import com.boxai.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;


import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户画像控制器
 * 提供用户画像分析和行为记录功能
 */
@RestController
@RequestMapping("/api/user-profile")
@Validated
@RequiredArgsConstructor
@Tag(name = "用户画像", description = "用户行为分析和画像统计相关API")
public class UserProfileController {
    
    private final UserProfileService userProfileService;

    /**
     * 获取当前用户画像分析
     * 包含偏好歌曲类型、使用频次、用户标签等详细分析
     * @return 用户画像分析结果
     */
    @GetMapping("/analysis")
    @Operation(summary = "获取当前用户画像分析", 
               description = "获取当前登录用户的详细画像分析，包括音乐偏好、使用习惯等")
    public ApiResponse<UserProfileAnalysisDTO> getUserProfileAnalysis() {
        Long userId = JwtAuthFilter.CURRENT_USER.get();
        return ApiResponse.ok(userProfileService.getUserProfileAnalysis(userId));
    }
    
    /**
     * 获取指定用户画像分析（管理员功能）
     * @param userId 用户ID
     * @return 用户画像分析结果
     */
    @GetMapping("/analysis/{userId}")
    @Operation(summary = "获取指定用户画像分析", 
               description = "管理员功能：获取指定用户的详细画像分析")
    public ApiResponse<UserProfileAnalysisDTO> getUserProfileAnalysis(
            @Parameter(description = "用户ID", required = true, example = "123")
            @PathVariable Long userId) {
        return ApiResponse.ok(userProfileService.getUserProfileAnalysis(userId));
    }
    
    /**
     * 手动更新用户画像
     * 重新计算用户的偏好和使用频次
     * @return 操作结果
     */
    @PostMapping("/refresh")
    @Operation(summary = "手动更新用户画像", 
               description = "重新分析用户行为数据，更新用户画像信息")

    public ApiResponse<String> refreshUserProfile() {
        Long userId = JwtAuthFilter.CURRENT_USER.get();
        userProfileService.updateUserProfile(userId);
        return ApiResponse.ok("用户画像已更新");
    }
    
    /**
     * 记录用户行为
     * 用于手动记录特定的用户行为，补充自动记录的不足
     * @param request 行为记录请求
     * @return 操作结果
     */
    @PostMapping("/behavior")
    @Operation(summary = "记录用户行为", 
               description = "手动记录用户的播放、点赞、搜索等行为，用于画像分析")
    public ApiResponse<String> recordBehavior(
            @Parameter(description = "行为记录请求参数", required = true)
            @RequestBody @Validated BehaviorRecordRequest request) {
        Long userId = JwtAuthFilter.CURRENT_USER.get();
        userProfileService.recordUserBehavior(
            userId,
            request.getBehaviorType(),
            request.getTargetId(),
            request.getTargetType(),
            request.getMetadata(),
            request.getSessionId()
        );
        return ApiResponse.ok("行为记录成功");
    }
    
    
}
