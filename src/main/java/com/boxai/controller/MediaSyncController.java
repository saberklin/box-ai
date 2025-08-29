package com.boxai.controller;

import com.boxai.common.web.ApiResponse;
import com.boxai.domain.entity.MediaSyncLog;
import com.boxai.domain.entity.Track;
import com.boxai.service.MediaSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 媒体同步控制器
 * 负责管理本地媒体文件的同步和更新
 */
@RestController
@RequestMapping("/api/media-sync")
@RequiredArgsConstructor
@Tag(name = "媒体同步管理", description = "本地媒体文件同步和管理功能")
public class MediaSyncController {
    
    private final MediaSyncService mediaSyncService;
    
    @PostMapping("/tracks/{trackId}/sync")
    @Operation(summary = "同步单个歌曲到本地", description = "将指定歌曲从云端下载到本地存储")
    public ApiResponse<Boolean> syncTrackToLocal(
            @Parameter(description = "歌曲ID", required = true) @PathVariable Long trackId,
            @Parameter(description = "包间ID", example = "1") @RequestParam(required = false) Long roomId
    ) {
        boolean success = mediaSyncService.syncTrackToLocal(trackId, roomId);
        return ApiResponse.success(success);
    }
    
    @PostMapping("/tracks/batch-sync")
    @Operation(summary = "批量同步歌曲到本地", description = "批量将歌曲从云端下载到本地存储")
    public ApiResponse<Integer> batchSyncTracksToLocal(
            @Parameter(description = "歌曲ID列表", required = true) @RequestBody List<Long> trackIds,
            @Parameter(description = "包间ID", example = "1") @RequestParam(required = false) Long roomId
    ) {
        int successCount = mediaSyncService.batchSyncTracksToLocal(trackIds, roomId);
        return ApiResponse.success(successCount);
    }
    
    @GetMapping("/tracks/{trackId}/exists")
    @Operation(summary = "检查本地文件是否存在", description = "检查指定歌曲的本地文件是否存在")
    public ApiResponse<Boolean> isLocalFileExists(
            @Parameter(description = "歌曲ID", required = true) @PathVariable Long trackId
    ) {
        boolean exists = mediaSyncService.isLocalFileExists(trackId);
        return ApiResponse.success(exists);
    }
    
    @GetMapping("/tracks/{trackId}/path")
    @Operation(summary = "获取本地文件路径", description = "获取指定歌曲的本地文件路径")
    public ApiResponse<String> getLocalFilePath(
            @Parameter(description = "歌曲ID", required = true) @PathVariable Long trackId
    ) {
        String path = mediaSyncService.getLocalFilePath(trackId);
        return ApiResponse.success(path);
    }
    
    @DeleteMapping("/tracks/{trackId}/local")
    @Operation(summary = "删除本地文件", description = "删除指定歌曲的本地文件")
    public ApiResponse<Boolean> deleteLocalFile(
            @Parameter(description = "歌曲ID", required = true) @PathVariable Long trackId
    ) {
        boolean deleted = mediaSyncService.deleteLocalFile(trackId);
        return ApiResponse.success(deleted);
    }
    
    @PostMapping("/sync-updated")
    @Operation(summary = "同步更新的歌曲", description = "检查并同步云端已更新的歌曲")
    public ApiResponse<Integer> syncUpdatedTracks(
            @Parameter(description = "包间ID", example = "1") @RequestParam(required = false) Long roomId
    ) {
        int syncCount = mediaSyncService.syncUpdatedTracks(roomId);
        return ApiResponse.success(syncCount);
    }
    
    @GetMapping("/storage-info")
    @Operation(summary = "获取存储空间信息", description = "获取本地存储空间使用情况")
    public ApiResponse<MediaSyncService.StorageInfo> getStorageInfo() {
        MediaSyncService.StorageInfo info = mediaSyncService.getStorageInfo();
        return ApiResponse.success(info);
    }
    
    @PostMapping("/cleanup")
    @Operation(summary = "清理未使用的文件", description = "清理本地不再使用的媒体文件")
    public ApiResponse<Integer> cleanupUnusedFiles() {
        int cleanedCount = mediaSyncService.cleanupUnusedFiles();
        return ApiResponse.success(cleanedCount);
    }
    
    @GetMapping("/sync-history")
    @Operation(summary = "获取同步历史记录", description = "获取媒体文件同步的历史记录")
    public ApiResponse<List<MediaSyncLog>> getSyncHistory(
            @Parameter(description = "包间ID", example = "1") @RequestParam(required = false) Long roomId,
            @Parameter(description = "记录数量限制", example = "50") @RequestParam(defaultValue = "50") Integer limit
    ) {
        List<MediaSyncLog> history = mediaSyncService.getSyncHistory(roomId, limit);
        return ApiResponse.success(history);
    }
}
