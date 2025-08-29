package com.boxai.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.boxai.common.web.ApiResponse;
import com.boxai.domain.entity.LikeRecord;
import com.boxai.domain.entity.Track;
import com.boxai.service.LikeService;
import com.boxai.service.TrackService;
import com.boxai.domain.dto.request.TrackLikeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * 音乐曲目控制器
 * 提供曲目搜索、分类查询、喜欢/取消喜欢等功能
 */
@RestController
@RequestMapping("/api/tracks")
@Validated
@RequiredArgsConstructor
@Tag(name = "音乐曲目", description = "音乐曲目管理相关API")
public class TrackController {
    private final TrackService trackService;
    private final LikeService likeService;

    /**
     * 搜索音乐曲目
     * 根据关键词在曲名、艺术家、标签中搜索匹配的曲目
     * @param keyword 搜索关键词
     * @return 匹配的曲目列表
     */
    @GetMapping("/search")
    public ApiResponse<List<Track>> search(@RequestParam @NotBlank String keyword) {
        LambdaQueryWrapper<Track> qw = Wrappers.<Track>lambdaQuery()
                .like(Track::getTitle, keyword).or().like(Track::getArtist, keyword).or().like(Track::getTags, keyword);
        return ApiResponse.ok(trackService.list(qw));
    }

    /**
     * 按分类获取曲目
     * @param category 曲目分类（如流行、摇滚、民谣等）
     * @return 该分类下的所有曲目
     */
    @GetMapping("/category")
    public ApiResponse<List<Track>> byCategory(@RequestParam String category) {
        return ApiResponse.ok(trackService.list(Wrappers.<Track>lambdaQuery().eq(Track::getCategory, category)));
    }

    /**
     * 喜欢曲目
     * 用户对指定曲目表示喜欢，重复操作不会重复记录
     * @param req 喜欢请求（包含用户ID和曲目ID）
     * @return 操作结果
     */
    @PostMapping("/like")
    @Operation(summary = "点赞曲目", 
               description = "用户点赞指定曲目，重复点赞不会重复记录")

    public ApiResponse<String> like(
            @Parameter(description = "点赞请求参数", required = true)
            @RequestBody TrackLikeRequest req) {
        LikeRecord r = likeService.getOne(Wrappers.<LikeRecord>lambdaQuery()
                .eq(LikeRecord::getUserId, req.getUserId()).eq(LikeRecord::getTrackId, req.getTrackId()));
        if (r == null) {
            r = new LikeRecord();
            r.setUserId(req.getUserId());
            r.setTrackId(req.getTrackId());
            likeService.save(r);
        }
        return ApiResponse.ok("liked");
    }

    /**
     * 取消喜欢曲目
     * 用户取消对指定曲目的喜欢
     * @param req 取消喜欢请求（包含用户ID和曲目ID）
     * @return 操作结果
     */
    @DeleteMapping("/like")
    @Operation(summary = "取消点赞曲目", 
               description = "用户取消对指定曲目的点赞")

    public ApiResponse<String> unlike(
            @Parameter(description = "取消点赞请求参数", required = true)
            @RequestBody TrackLikeRequest req) {
        likeService.remove(Wrappers.<LikeRecord>lambdaQuery()
                .eq(LikeRecord::getUserId, req.getUserId()).eq(LikeRecord::getTrackId, req.getTrackId()));
        return ApiResponse.ok("unliked");
    }


}


