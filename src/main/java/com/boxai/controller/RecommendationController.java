package com.boxai.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boxai.common.web.ApiResponse;
import com.boxai.domain.dto.TrackRecommendationDto;
import com.boxai.domain.dto.PlaylistRecommendationDto;
import com.boxai.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 推荐系统控制器
 * 提供歌曲和歌单的智能推荐API
 */
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Tag(name = "推荐系统", description = "歌曲和歌单的智能推荐功能")
public class RecommendationController {
    
    private final RecommendationService recommendationService;
    
    @GetMapping("/tracks/personalized")
    @Operation(summary = "获取个性化推荐歌曲", description = "基于用户历史行为和偏好进行个性化推荐")
    public ApiResponse<List<TrackRecommendationDto>> getPersonalizedRecommendations(
            @Parameter(description = "用户ID", required = true) @RequestParam Long userId,
            @Parameter(description = "推荐数量限制", example = "20") @RequestParam(defaultValue = "20") Integer limit
    ) {
        List<TrackRecommendationDto> recommendations = recommendationService.getPersonalizedRecommendations(userId, limit);
        return ApiResponse.success(recommendations);
    }
    
    @GetMapping("/tracks/hot")
    @Operation(summary = "获取热门推荐歌曲", description = "基于全局热度进行推荐")
    public ApiResponse<List<TrackRecommendationDto>> getHotRecommendations(
            @Parameter(description = "分类筛选", example = "流行") @RequestParam(required = false) String category,
            @Parameter(description = "推荐数量限制", example = "20") @RequestParam(defaultValue = "20") Integer limit
    ) {
        List<TrackRecommendationDto> recommendations = recommendationService.getHotRecommendations(category, limit);
        return ApiResponse.success(recommendations);
    }
    
    @GetMapping("/tracks/new")
    @Operation(summary = "获取新歌推荐", description = "推荐最新上线的歌曲")
    public ApiResponse<List<TrackRecommendationDto>> getNewSongRecommendations(
            @Parameter(description = "推荐数量限制", example = "15") @RequestParam(defaultValue = "15") Integer limit
    ) {
        List<TrackRecommendationDto> recommendations = recommendationService.getNewSongRecommendations(limit);
        return ApiResponse.success(recommendations);
    }
    
    @GetMapping("/tracks/{trackId}/similar")
    @Operation(summary = "获取相似歌曲推荐", description = "基于指定歌曲推荐相似的歌曲")
    public ApiResponse<List<TrackRecommendationDto>> getSimilarTracks(
            @Parameter(description = "基准歌曲ID", required = true) @PathVariable Long trackId,
            @Parameter(description = "推荐数量限制", example = "10") @RequestParam(defaultValue = "10") Integer limit
    ) {
        List<TrackRecommendationDto> recommendations = recommendationService.getSimilarTracks(trackId, limit);
        return ApiResponse.success(recommendations);
    }
    
    @GetMapping("/playlists")
    @Operation(summary = "获取推荐歌单列表", description = "返回系统生成的各类推荐歌单")
    public ApiResponse<List<PlaylistRecommendationDto>> getRecommendationPlaylists() {
        List<PlaylistRecommendationDto> playlists = recommendationService.getRecommendationPlaylists();
        return ApiResponse.success(playlists);
    }
    
    @GetMapping("/playlists/{playlistId}/tracks")
    @Operation(summary = "获取推荐歌单详情", description = "获取指定推荐歌单的详细信息和歌曲列表")
    public ApiResponse<Page<TrackRecommendationDto>> getPlaylistTracks(
            @Parameter(description = "歌单ID", required = true) @PathVariable Long playlistId,
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页数量", example = "20") @RequestParam(defaultValue = "20") Integer size
    ) {
        Page<TrackRecommendationDto> page = new Page<>(current, size);
        Page<TrackRecommendationDto> result = recommendationService.getPlaylistTracks(playlistId, page);
        return ApiResponse.success(result);
    }
    
    @GetMapping("/rankings/{rankingType}")
    @Operation(summary = "获取热门榜单", description = "获取指定类型的热门榜单")
    public ApiResponse<List<TrackRecommendationDto>> getHotRanking(
            @Parameter(description = "榜单类型", example = "WEEKLY", required = true) @PathVariable String rankingType,
            @Parameter(description = "分类筛选", example = "流行") @RequestParam(required = false) String category,
            @Parameter(description = "榜单数量限制", example = "50") @RequestParam(defaultValue = "50") Integer limit
    ) {
        List<TrackRecommendationDto> rankings = recommendationService.getHotRanking(rankingType, category, limit);
        return ApiResponse.success(rankings);
    }
    
    @PostMapping("/preferences/update")
    @Operation(summary = "更新用户偏好", description = "基于用户行为更新偏好模型")
    public ApiResponse<Void> updateUserPreference(
            @Parameter(description = "用户ID", required = true) @RequestParam Long userId,
            @Parameter(description = "歌曲ID", required = true) @RequestParam Long trackId,
            @Parameter(description = "行为类型", example = "PLAY", required = true) @RequestParam String action
    ) {
        recommendationService.updateUserPreference(userId, trackId, action);
        return ApiResponse.success();
    }
    
    @PostMapping("/admin/refresh-rankings")
    @Operation(summary = "刷新热门榜单", description = "管理员手动刷新热门榜单（正常情况下由定时任务执行）")
    public ApiResponse<Void> refreshHotRankings() {
        recommendationService.refreshHotRankings();
        return ApiResponse.success();
    }
    
    @PostMapping("/admin/refresh-playlists")
    @Operation(summary = "刷新推荐歌单", description = "管理员手动刷新推荐歌单（正常情况下由定时任务执行）")
    public ApiResponse<Void> refreshRecommendationPlaylists() {
        recommendationService.refreshRecommendationPlaylists();
        return ApiResponse.success();
    }
}
