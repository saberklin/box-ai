package com.boxai.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.boxai.auth.JwtAuthFilter;
import com.boxai.common.web.ApiResponse;
import com.boxai.domain.entity.UserPlaylist;
import com.boxai.domain.entity.UserPlaylistItem;
import com.boxai.service.UserPlaylistItemService;
import com.boxai.service.UserPlaylistService;
import com.boxai.domain.dto.request.PlaylistCreateRequest;
import com.boxai.domain.dto.request.PlaylistAddItemRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户播放列表控制器
 * 提供用户个人播放列表的创建、管理和曲目操作功能
 */
@RestController
@RequestMapping("/api/user-playlists")
@RequiredArgsConstructor
@Tag(name = "用户播放列表", description = "用户自定义播放列表管理相关API")
public class UserPlaylistController {
    private final UserPlaylistService playlistService;
    private final UserPlaylistItemService itemService;

    /**
     * 创建用户播放列表
     * 为当前用户创建一个新的个人播放列表
     * @param req 创建请求（包含播放列表名称）
     *            +
     *
     * @return 创建的播放列表信息
     */
    @PostMapping
    @Operation(summary = "创建播放列表", 
               description = "用户创建一个新的个人播放列表")

    public ApiResponse<UserPlaylist> create(
            @Parameter(description = "创建播放列表请求参数", required = true)
            @RequestBody PlaylistCreateRequest req) {
        UserPlaylist p = new UserPlaylist();
        p.setUserId(JwtAuthFilter.CURRENT_USER.get());
        p.setName(req.getName());
        playlistService.save(p);
        return ApiResponse.ok(p);
    }

    /**
     * 获取当前用户的所有播放列表
     * @return 用户播放列表列表
     */
    @GetMapping
    public ApiResponse<List<UserPlaylist>> myPlaylists() {
        Long uid = JwtAuthFilter.CURRENT_USER.get();
        return ApiResponse.ok(playlistService.list(Wrappers.<UserPlaylist>lambdaQuery().eq(UserPlaylist::getUserId, uid)));
    }

    /**
     * 向播放列表添加曲目
     * 将指定曲目添加到用户的播放列表中
     * @param playlistId 播放列表ID
     * @param req 添加请求（包含曲目ID和位置）
     * @return 添加的播放列表项
     */
    @PostMapping("/{playlistId}/items")
    @Operation(summary = "添加曲目到播放列表", 
               description = "向指定播放列表中添加一首曲目")

    public ApiResponse<UserPlaylistItem> addItem(
            @Parameter(description = "播放列表ID", required = true, example = "1")
            @PathVariable Long playlistId, 
            @Parameter(description = "添加曲目请求参数", required = true)
            @RequestBody PlaylistAddItemRequest req) {
        UserPlaylistItem item = new UserPlaylistItem();
        item.setPlaylistId(playlistId);
        item.setTrackId(req.getTrackId());
        item.setPosition(req.getPosition());
        itemService.save(item);
        return ApiResponse.ok(item);
    }

    /**
     * 从播放列表中移除曲目
     * 删除播放列表中的指定曲目
     * @param playlistId 播放列表ID
     * @param itemId 播放列表项ID
     * @return 删除结果
     */
    @DeleteMapping("/{playlistId}/items/{itemId}")
    public ApiResponse<String> removeItem(@PathVariable Long playlistId, @PathVariable Long itemId) {
        itemService.remove(Wrappers.<UserPlaylistItem>lambdaQuery().eq(UserPlaylistItem::getId, itemId).eq(UserPlaylistItem::getPlaylistId, playlistId));
        return ApiResponse.ok("deleted");
    }


}


