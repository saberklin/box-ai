package com.boxai.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boxai.common.web.ApiResponse;
import com.boxai.domain.dto.PlaylistRecommendationDto;
import com.boxai.domain.dto.TrackRecommendationDto;
import com.boxai.domain.entity.Track;
import com.boxai.service.RecommendationService;
import com.boxai.service.TrackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 前端API控制器
 * 为小程序前端提供统一的API接口
 */
@RestController
@RequestMapping("/api/frontend")
@RequiredArgsConstructor
@Tag(name = "前端API", description = "专为小程序前端设计的统一API接口")
public class FrontendApiController {
    
    private final RecommendationService recommendationService;
    private final TrackService trackService;
    
    @GetMapping("/home")
    @Operation(summary = "获取首页数据", description = "获取首页所需的所有数据，包括推荐歌单、热门歌曲、新歌推荐等")
    public ApiResponse<Map<String, Object>> getHomeData(
            @Parameter(description = "用户ID", example = "1") @RequestParam(required = false) Long userId
    ) {
        Map<String, Object> homeData = new HashMap<>();
        
        // 推荐歌单
        List<PlaylistRecommendationDto> recommendPlaylists = recommendationService.getRecommendationPlaylists();
        homeData.put("recommendPlaylists", recommendPlaylists);
        
        // 热门歌曲
        List<TrackRecommendationDto> hotTracks = recommendationService.getHotRecommendations(null, 10);
        homeData.put("hotTracks", hotTracks);
        
        // 新歌推荐
        List<TrackRecommendationDto> newTracks = recommendationService.getNewSongRecommendations(8);
        homeData.put("newTracks", newTracks);
        
        // 个性化推荐（如果用户已登录）
        if (userId != null) {
            List<TrackRecommendationDto> personalizedTracks = 
                recommendationService.getPersonalizedRecommendations(userId, 12);
            homeData.put("personalizedTracks", personalizedTracks);
        }
        
        return ApiResponse.success(homeData);
    }
    
    @GetMapping("/discover")
    @Operation(summary = "获取发现页数据", description = "获取发现页的推荐内容")
    public ApiResponse<Map<String, Object>> getDiscoverData() {
        Map<String, Object> discoverData = new HashMap<>();
        
        // 各类榜单
        List<TrackRecommendationDto> weeklyHot = recommendationService.getHotRanking("WEEKLY", null, 20);
        List<TrackRecommendationDto> monthlyHot = recommendationService.getHotRanking("MONTHLY", null, 20);
        
        discoverData.put("weeklyHot", weeklyHot);
        discoverData.put("monthlyHot", monthlyHot);
        
        // 分类推荐
        List<TrackRecommendationDto> popTracks = recommendationService.getHotRecommendations("流行", 10);
        List<TrackRecommendationDto> rockTracks = recommendationService.getHotRecommendations("摇滚", 10);
        List<TrackRecommendationDto> folkTracks = recommendationService.getHotRecommendations("民谣", 10);
        
        discoverData.put("popTracks", popTracks);
        discoverData.put("rockTracks", rockTracks);
        discoverData.put("folkTracks", folkTracks);
        
        return ApiResponse.success(discoverData);
    }
    
    @GetMapping("/search/suggestions")
    @Operation(summary = "获取搜索建议", description = "根据关键词获取搜索建议")
    public ApiResponse<List<String>> getSearchSuggestions(
            @Parameter(description = "搜索关键词", required = true) @RequestParam String keyword
    ) {
        // 这里可以实现搜索建议逻辑
        // 目前返回简单的示例数据
        List<String> suggestions = List.of(
            keyword + " - 热门歌手",
            keyword + " - 经典歌曲",
            keyword + " - 新歌推荐"
        );
        
        return ApiResponse.success(suggestions);
    }
    
    @GetMapping("/tracks/search")
    @Operation(summary = "搜索歌曲", description = "根据关键词搜索歌曲")
    public ApiResponse<Page<Track>> searchTracks(
            @Parameter(description = "搜索关键词", required = true) @RequestParam String keyword,
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页数量", example = "20") @RequestParam(defaultValue = "20") Integer size
    ) {
        Page<Track> page = new Page<>(current, size);
        Page<Track> result = trackService.searchTracks(keyword, page);
        return ApiResponse.success(result);
    }
    
    @GetMapping("/categories")
    @Operation(summary = "获取音乐分类", description = "获取所有音乐分类列表")
    public ApiResponse<Map<String, List<String>>> getCategories() {
        Map<String, List<String>> categories = new HashMap<>();
        
        categories.put("genre", List.of("流行", "摇滚", "民谣", "说唱", "电子", "古典", "爵士", "蓝调"));
        categories.put("language", List.of("中文", "英文", "日文", "韩文", "粤语", "闽南语", "其他"));
        categories.put("mood", List.of("快乐", "伤感", "怀旧", "励志", "浪漫", "安静", "激昂", "治愈"));
        categories.put("scene", List.of("聚会", "独处", "运动", "学习", "睡前", "开车", "约会", "工作"));
        
        return ApiResponse.success(categories);
    }
    
    @GetMapping("/tracks/by-category")
    @Operation(summary = "按分类获取歌曲", description = "根据分类获取歌曲列表")
    public ApiResponse<List<TrackRecommendationDto>> getTracksByCategory(
            @Parameter(description = "分类类型", example = "genre") @RequestParam String categoryType,
            @Parameter(description = "分类值", example = "流行") @RequestParam String categoryValue,
            @Parameter(description = "数量限制", example = "20") @RequestParam(defaultValue = "20") Integer limit
    ) {
        List<TrackRecommendationDto> tracks;
        
        if ("genre".equals(categoryType)) {
            tracks = recommendationService.getHotRecommendations(categoryValue, limit);
        } else {
            // 其他分类的处理逻辑
            tracks = recommendationService.getHotRecommendations(null, limit);
        }
        
        return ApiResponse.success(tracks);
    }
    
    @GetMapping("/playlists/{playlistId}")
    @Operation(summary = "获取歌单详情", description = "获取指定歌单的详细信息和歌曲列表")
    public ApiResponse<Map<String, Object>> getPlaylistDetail(
            @Parameter(description = "歌单ID", required = true) @PathVariable Long playlistId,
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页数量", example = "20") @RequestParam(defaultValue = "20") Integer size
    ) {
        Map<String, Object> playlistDetail = new HashMap<>();
        
        // 获取歌单信息和歌曲列表
        Page<TrackRecommendationDto> page = new Page<>(current, size);
        Page<TrackRecommendationDto> tracks = recommendationService.getPlaylistTracks(playlistId, page);
        
        playlistDetail.put("tracks", tracks);
        
        return ApiResponse.success(playlistDetail);
    }
    
    @PostMapping("/user/{userId}/behavior")
    @Operation(summary = "记录用户行为", description = "记录用户的播放、点赞、收藏等行为，用于推荐算法优化")
    public ApiResponse<Void> recordUserBehavior(
            @Parameter(description = "用户ID", required = true) @PathVariable Long userId,
            @Parameter(description = "歌曲ID", required = true) @RequestParam Long trackId,
            @Parameter(description = "行为类型", example = "PLAY") @RequestParam String action
    ) {
        recommendationService.updateUserPreference(userId, trackId, action);
        return ApiResponse.success();
    }
    
    @GetMapping("/stats")
    @Operation(summary = "获取系统统计信息", description = "获取系统的统计信息，如歌曲总数、用户数等")
    public ApiResponse<Map<String, Object>> getSystemStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 这里可以添加实际的统计逻辑
        stats.put("totalTracks", 50000);
        stats.put("totalUsers", 10000);
        stats.put("totalPlaylists", 500);
        stats.put("todayPlays", 8888);
        
        return ApiResponse.success(stats);
    }
}
