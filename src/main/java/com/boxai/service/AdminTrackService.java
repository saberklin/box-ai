package com.boxai.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boxai.domain.dto.MediaUploadResponse;
import com.boxai.domain.dto.request.MediaUploadRequest;
import com.boxai.domain.dto.request.SyncVersionUpdateRequest;
import com.boxai.domain.dto.request.TrackCreateRequest;
import com.boxai.domain.dto.request.TrackUpdateRequest;
import com.boxai.domain.entity.Track;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 云端管理员曲库管理服务接口
 * 提供云端管理员专用的曲库管理功能
 */
public interface AdminTrackService {
    
    /**
     * 创建新歌曲
     * 
     * @param request 歌曲创建请求
     * @return 创建的歌曲信息
     */
    Track createTrack(TrackCreateRequest request);
    
    /**
     * 更新歌曲信息
     * 
     * @param request 歌曲更新请求
     * @return 更新后的歌曲信息
     */
    Track updateTrack(TrackUpdateRequest request);
    
    /**
     * 删除歌曲（软删除）
     * 
     * @param trackId 歌曲ID
     * @param reason 删除原因
     */
    void deleteTrack(Long trackId, String reason);
    
    /**
     * 上传媒体文件到CDN
     * 
     * @param file 文件
     * @param request 上传请求信息
     * @return 上传结果
     */
    MediaUploadResponse uploadMediaFile(MultipartFile file, MediaUploadRequest request);
    
    /**
     * 批量更新同步版本号
     * 
     * @param request 同步版本更新请求
     * @return 更新结果统计
     */
    Map<String, Object> updateSyncVersion(SyncVersionUpdateRequest request);
    
    /**
     * 获取待同步的歌曲列表
     * 
     * @param page 页码
     * @param size 每页大小
     * @param roomId 包间ID（可选）
     * @return 待同步歌曲列表
     */
    Page<Track> getPendingSyncTracks(int page, int size, Long roomId);
    
    /**
     * 强制推送更新到包间
     * 
     * @param trackIds 歌曲ID列表
     * @param roomIds 目标包间ID列表（为空则推送到所有包间）
     * @return 推送结果
     */
    Map<String, Object> forcePushUpdates(List<Long> trackIds, List<Long> roomIds);
    
    /**
     * 获取曲库统计信息
     * 
     * @param days 统计天数
     * @return 统计信息
     */
    Map<String, Object> getTrackStatistics(int days);
    
    /**
     * 获取同步日志
     * 
     * @param page 页码
     * @param size 每页大小
     * @param trackId 歌曲ID
     * @param roomId 包间ID
     * @param syncStatus 同步状态
     * @return 同步日志列表
     */
    Page<Map<String, Object>> getSyncLogs(int page, int size, Long trackId, Long roomId, String syncStatus);
    
    /**
     * 批量导入歌曲
     * 
     * @param file 导入文件
     * @param overwrite 是否覆盖已存在的歌曲
     * @return 导入结果
     */
    Map<String, Object> batchImportTracks(MultipartFile file, boolean overwrite);
    
    /**
     * 导出曲库数据
     * 
     * @param format 导出格式
     * @param status 状态过滤
     * @param category 分类过滤
     * @return 下载URL
     */
    String exportTracks(String format, String status, String category);
}
