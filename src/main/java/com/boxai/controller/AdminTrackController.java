package com.boxai.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boxai.common.web.ApiResponse;
import com.boxai.domain.dto.MediaUploadResponse;
import com.boxai.domain.dto.request.MediaUploadRequest;
import com.boxai.domain.dto.request.SyncVersionUpdateRequest;
import com.boxai.domain.dto.request.TrackCreateRequest;
import com.boxai.domain.dto.request.TrackUpdateRequest;
import com.boxai.domain.entity.Track;
import com.boxai.service.AdminTrackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 云端管理员曲库管理控制器
 * 专用于云端管理员操作，包括曲库的增删改查、媒体文件上传、同步版本管理等
 */
@RestController
@RequestMapping("/api/admin/tracks")
@RequiredArgsConstructor
@Tag(name = "云端管理员曲库管理", description = "云端管理员专用的曲库管理功能，包括歌曲管理、媒体上传、同步控制")
public class AdminTrackController {
    
    private final AdminTrackService adminTrackService;
    
    @PostMapping
    @Operation(summary = "新增歌曲", description = "云端管理员新增歌曲到曲库，会自动分配同步版本号")
    public ApiResponse<Track> createTrack(
            @Parameter(description = "歌曲创建信息", required = true) @Valid @RequestBody TrackCreateRequest request
    ) {
        Track track = adminTrackService.createTrack(request);
        return ApiResponse.success(track);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "更新歌曲信息", description = "更新歌曲基本信息，可选择是否更新同步版本号")
    public ApiResponse<Track> updateTrack(
            @Parameter(description = "歌曲ID", required = true) @PathVariable Long id,
            @Parameter(description = "歌曲更新信息", required = true) @Valid @RequestBody TrackUpdateRequest request
    ) {
        request.setId(id);
        Track track = adminTrackService.updateTrack(request);
        return ApiResponse.success(track);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "删除歌曲", description = "软删除歌曲，将状态标记为DELETED并更新同步版本")
    public ApiResponse<Void> deleteTrack(
            @Parameter(description = "歌曲ID", required = true) @PathVariable Long id,
            @Parameter(description = "删除原因") @RequestParam(required = false) String reason
    ) {
        adminTrackService.deleteTrack(id, reason);
        return ApiResponse.success();
    }
    
    @PostMapping("/{id}/media/upload")
    @Operation(summary = "上传媒体文件", description = "为指定歌曲上传媒体文件到CDN，支持视频、音频、封面等类型")
    public ApiResponse<MediaUploadResponse> uploadMediaFile(
            @Parameter(description = "歌曲ID", required = true) @PathVariable Long id,
            @Parameter(description = "媒体文件", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "上传请求信息", required = true) @Valid @ModelAttribute MediaUploadRequest request
    ) {
        request.setTrackId(id);
        MediaUploadResponse response = adminTrackService.uploadMediaFile(file, request);
        return ApiResponse.success(response);
    }
    
    @PostMapping("/sync-version/update")
    @Operation(summary = "批量更新同步版本", description = "批量更新歌曲的同步版本号，触发包间同步更新")
    public ApiResponse<Map<String, Object>> updateSyncVersion(
            @Parameter(description = "同步版本更新请求", required = true) @Valid @RequestBody SyncVersionUpdateRequest request
    ) {
        Map<String, Object> result = adminTrackService.updateSyncVersion(request);
        return ApiResponse.success(result);
    }
    
    @GetMapping("/sync-version/pending")
    @Operation(summary = "获取待同步歌曲列表", description = "获取需要推送到包间的歌曲更新列表")
    public ApiResponse<Page<Track>> getPendingSyncTracks(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小", example = "20") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "指定包间ID") @RequestParam(required = false) Long roomId
    ) {
        Page<Track> tracks = adminTrackService.getPendingSyncTracks(page, size, roomId);
        return ApiResponse.success(tracks);
    }
    
    @PostMapping("/sync-version/force-push")
    @Operation(summary = "强制推送更新", description = "强制推送指定歌曲更新到所有或指定包间")
    public ApiResponse<Map<String, Object>> forcePushUpdates(
            @Parameter(description = "歌曲ID列表", required = true) @RequestBody List<Long> trackIds,
            @Parameter(description = "目标包间ID列表") @RequestParam(required = false) List<Long> roomIds
    ) {
        Map<String, Object> result = adminTrackService.forcePushUpdates(trackIds, roomIds);
        return ApiResponse.success(result);
    }
    
    @GetMapping("/statistics")
    @Operation(summary = "获取曲库统计信息", description = "获取曲库的统计数据，包括总数、分类统计、同步状态等")
    public ApiResponse<Map<String, Object>> getTrackStatistics(
            @Parameter(description = "统计日期范围（天）", example = "30") @RequestParam(defaultValue = "30") int days
    ) {
        Map<String, Object> statistics = adminTrackService.getTrackStatistics(days);
        return ApiResponse.success(statistics);
    }
    
    @GetMapping("/sync-logs")
    @Operation(summary = "获取同步日志", description = "获取歌曲同步到各包间的日志记录")
    public ApiResponse<Page<Map<String, Object>>> getSyncLogs(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小", example = "20") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "歌曲ID") @RequestParam(required = false) Long trackId,
            @Parameter(description = "包间ID") @RequestParam(required = false) Long roomId,
            @Parameter(description = "同步状态") @RequestParam(required = false) String syncStatus
    ) {
        Page<Map<String, Object>> logs = adminTrackService.getSyncLogs(page, size, trackId, roomId, syncStatus);
        return ApiResponse.success(logs);
    }
    
    @PostMapping("/batch-import")
    @Operation(summary = "批量导入歌曲", description = "通过Excel或CSV文件批量导入歌曲信息")
    public ApiResponse<Map<String, Object>> batchImportTracks(
            @Parameter(description = "导入文件", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "是否覆盖已存在的歌曲", example = "false") @RequestParam(defaultValue = "false") boolean overwrite
    ) {
        Map<String, Object> result = adminTrackService.batchImportTracks(file, overwrite);
        return ApiResponse.success(result);
    }
    
    @GetMapping("/export")
    @Operation(summary = "导出曲库数据", description = "导出曲库数据到Excel文件")
    public ApiResponse<String> exportTracks(
            @Parameter(description = "导出格式", example = "EXCEL") @RequestParam(defaultValue = "EXCEL") String format,
            @Parameter(description = "歌曲状态过滤") @RequestParam(required = false) String status,
            @Parameter(description = "分类过滤") @RequestParam(required = false) String category
    ) {
        String downloadUrl = adminTrackService.exportTracks(format, status, category);
        return ApiResponse.success(downloadUrl);
    }
}
