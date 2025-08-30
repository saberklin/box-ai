package com.boxai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boxai.domain.dto.MediaUploadResponse;
import com.boxai.domain.dto.request.MediaUploadRequest;
import com.boxai.domain.dto.request.SyncVersionUpdateRequest;
import com.boxai.domain.dto.request.TrackCreateRequest;
import com.boxai.domain.dto.request.TrackUpdateRequest;
import com.boxai.domain.entity.MediaSyncLog;
import com.boxai.domain.entity.Track;
import com.boxai.domain.mapper.MediaSyncLogMapper;
import com.boxai.domain.mapper.TrackMapper;
import com.boxai.service.AdminTrackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 云端管理员曲库管理服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminTrackServiceImpl implements AdminTrackService {
    
    private final TrackMapper trackMapper;
    private final MediaSyncLogMapper mediaSyncLogMapper;
    
    @Override
    @Transactional
    public Track createTrack(TrackCreateRequest request) {
        Track track = new Track();
        BeanUtils.copyProperties(request, track);
        
        // 设置初始同步版本
        track.setSyncVersion(1L);
        track.setLastSyncAt(LocalDateTime.now());
        track.setPlayCount(0L);
        track.setLikeCount(0);
        track.setHotScore(0);
        track.setRecentPlayCount(0);
        
        trackMapper.insert(track);
        
        log.info("管理员创建歌曲成功: trackId={}, title={}", track.getId(), track.getTitle());
        return track;
    }
    
    @Override
    @Transactional
    public Track updateTrack(TrackUpdateRequest request) {
        Track existingTrack = trackMapper.selectById(request.getId());
        if (existingTrack == null) {
            throw new RuntimeException("歌曲不存在: " + request.getId());
        }
        
        // 复制非空字段
        Track updateTrack = new Track();
        BeanUtils.copyProperties(request, updateTrack);
        updateTrack.setId(request.getId());
        
        // 如果强制更新版本或有重要字段变更，则更新同步版本
        if (request.getForceUpdateVersion() || hasImportantFieldChanged(existingTrack, updateTrack)) {
            updateTrack.setSyncVersion(existingTrack.getSyncVersion() + 1);
            updateTrack.setLastSyncAt(LocalDateTime.now());
        }
        
        trackMapper.updateById(updateTrack);
        
        log.info("管理员更新歌曲成功: trackId={}, newVersion={}", request.getId(), updateTrack.getSyncVersion());
        return trackMapper.selectById(request.getId());
    }
    
    @Override
    @Transactional
    public void deleteTrack(Long trackId, String reason) {
        Track track = trackMapper.selectById(trackId);
        if (track == null) {
            throw new RuntimeException("歌曲不存在: " + trackId);
        }
        
        // 软删除：更新状态和同步版本
        track.setStatus("DELETED");
        track.setSyncVersion(track.getSyncVersion() + 1);
        track.setLastSyncAt(LocalDateTime.now());
        
        trackMapper.updateById(track);
        
        log.info("管理员删除歌曲成功: trackId={}, reason={}", trackId, reason);
    }
    
    @Override
    @Transactional
    public MediaUploadResponse uploadMediaFile(MultipartFile file, MediaUploadRequest request) {
        // 模拟文件上传到CDN的过程
        String uploadId = "upload_" + System.currentTimeMillis();
        String cdnUrl = generateCdnUrl(request.getTrackId(), request.getFileType(), file.getOriginalFilename());
        
        try {
            // 这里应该实现实际的CDN上传逻辑
            // 1. 验证文件格式和大小
            // 2. 上传到CDN存储
            // 3. 获取CDN URL
            
            // 模拟上传过程
            Thread.sleep(1000); // 模拟上传时间
            
            // 更新歌曲的媒体文件信息
            Track track = trackMapper.selectById(request.getTrackId());
            if (track == null) {
                throw new RuntimeException("歌曲不存在: " + request.getTrackId());
            }
            
            // 根据文件类型更新相应字段
            updateTrackMediaInfo(track, request.getFileType(), cdnUrl, file.getSize(), request.getQuality());
            
            // 更新同步版本
            track.setSyncVersion(track.getSyncVersion() + 1);
            track.setLastSyncAt(LocalDateTime.now());
            trackMapper.updateById(track);
            
            MediaUploadResponse response = new MediaUploadResponse();
            response.setUploadId(uploadId);
            response.setCdnUrl(cdnUrl);
            response.setFileSize(file.getSize());
            response.setUploadStatus("SUCCESS");
            response.setMessage("文件上传成功");
            response.setFileMd5(request.getFileMd5());
            
            log.info("媒体文件上传成功: trackId={}, fileType={}, cdnUrl={}", 
                    request.getTrackId(), request.getFileType(), cdnUrl);
            
            return response;
            
        } catch (Exception e) {
            log.error("媒体文件上传失败: trackId={}, fileType={}", request.getTrackId(), request.getFileType(), e);
            
            MediaUploadResponse response = new MediaUploadResponse();
            response.setUploadId(uploadId);
            response.setUploadStatus("FAILED");
            response.setMessage("文件上传失败: " + e.getMessage());
            return response;
        }
    }
    
    @Override
    @Transactional
    public Map<String, Object> updateSyncVersion(SyncVersionUpdateRequest request) {
        int successCount = 0;
        int failedCount = 0;
        List<String> errors = new ArrayList<>();
        
        for (Long trackId : request.getTrackIds()) {
            try {
                Track track = trackMapper.selectById(trackId);
                if (track == null) {
                    errors.add("歌曲不存在: " + trackId);
                    failedCount++;
                    continue;
                }
                
                // 更新同步版本
                track.setSyncVersion(track.getSyncVersion() + 1);
                track.setLastSyncAt(LocalDateTime.now());
                trackMapper.updateById(track);
                
                successCount++;
                
            } catch (Exception e) {
                errors.add("更新歌曲 " + trackId + " 失败: " + e.getMessage());
                failedCount++;
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failedCount", failedCount);
        result.put("errors", errors);
        result.put("reason", request.getReason());
        result.put("updateTime", LocalDateTime.now());
        
        log.info("批量更新同步版本完成: 成功={}, 失败={}, 原因={}", 
                successCount, failedCount, request.getReason());
        
        return result;
    }
    
    @Override
    public Page<Track> getPendingSyncTracks(int page, int size, Long roomId) {
        Page<Track> trackPage = new Page<>(page, size);
        QueryWrapper<Track> queryWrapper = new QueryWrapper<>();
        
        // 查询有更新的歌曲（这里简化处理，实际应该根据包间的同步状态来判断）
        queryWrapper.eq("status", "ACTIVE")
                   .orderByDesc("last_sync_at");
        
        if (roomId != null) {
            // 这里应该关联查询包间的同步状态
            // 简化处理，返回所有活跃歌曲
        }
        
        return trackMapper.selectPage(trackPage, queryWrapper);
    }
    
    @Override
    public Map<String, Object> forcePushUpdates(List<Long> trackIds, List<Long> roomIds) {
        // 模拟强制推送逻辑
        Map<String, Object> result = new HashMap<>();
        result.put("pushedTrackCount", trackIds.size());
        result.put("targetRoomCount", roomIds != null ? roomIds.size() : "ALL");
        result.put("pushTime", LocalDateTime.now());
        result.put("status", "SUCCESS");
        
        log.info("强制推送更新完成: tracks={}, rooms={}", trackIds.size(), 
                roomIds != null ? roomIds.size() : "ALL");
        
        return result;
    }
    
    @Override
    public Map<String, Object> getTrackStatistics(int days) {
        // 模拟统计数据
        Map<String, Object> statistics = new HashMap<>();
        
        // 总体统计
        Long totalTracks = trackMapper.selectCount(new QueryWrapper<Track>().eq("status", "ACTIVE"));
        Long newTracks = trackMapper.selectCount(new QueryWrapper<Track>()
                .eq("status", "ACTIVE")
                .ge("created_at", LocalDateTime.now().minusDays(days)));
        
        statistics.put("totalTracks", totalTracks);
        statistics.put("newTracks", newTracks);
        statistics.put("activeTracks", totalTracks);
        statistics.put("deletedTracks", trackMapper.selectCount(new QueryWrapper<Track>().eq("status", "DELETED")));
        
        // 分类统计
        Map<String, Long> categoryStats = new HashMap<>();
        categoryStats.put("流行", 1500L);
        categoryStats.put("摇滚", 800L);
        categoryStats.put("民谣", 600L);
        categoryStats.put("电子", 400L);
        statistics.put("categoryStats", categoryStats);
        
        // 语言统计
        Map<String, Long> languageStats = new HashMap<>();
        languageStats.put("中文", 2000L);
        languageStats.put("英文", 1000L);
        languageStats.put("日文", 200L);
        languageStats.put("韩文", 100L);
        statistics.put("languageStats", languageStats);
        
        statistics.put("statisticsDate", LocalDateTime.now());
        statistics.put("statisticsDays", days);
        
        return statistics;
    }
    
    @Override
    public Page<Map<String, Object>> getSyncLogs(int page, int size, Long trackId, Long roomId, String syncStatus) {
        Page<MediaSyncLog> logPage = new Page<>(page, size);
        QueryWrapper<MediaSyncLog> queryWrapper = new QueryWrapper<>();
        
        if (trackId != null) {
            queryWrapper.eq("track_id", trackId);
        }
        if (roomId != null) {
            queryWrapper.eq("room_id", roomId);
        }
        if (syncStatus != null) {
            queryWrapper.eq("sync_status", syncStatus);
        }
        
        queryWrapper.orderByDesc("started_at");
        
        Page<MediaSyncLog> logs = mediaSyncLogMapper.selectPage(logPage, queryWrapper);
        
        // 转换为Map格式
        Page<Map<String, Object>> result = new Page<>(page, size);
        result.setTotal(logs.getTotal());
        
        List<Map<String, Object>> records = new ArrayList<>();
        for (MediaSyncLog log : logs.getRecords()) {
            Map<String, Object> record = new HashMap<>();
            record.put("id", log.getId());
            record.put("trackId", log.getTrackId());
            record.put("roomId", log.getRoomId());
            record.put("syncType", log.getSyncType());
            record.put("syncStatus", log.getSyncStatus());
            record.put("filePath", log.getFilePath());
            record.put("fileSize", log.getFileSize());
            record.put("errorMessage", log.getErrorMessage());
            record.put("startedAt", log.getStartedAt());
            record.put("completedAt", log.getCompletedAt());
            records.add(record);
        }
        result.setRecords(records);
        
        return result;
    }
    
    @Override
    public Map<String, Object> batchImportTracks(MultipartFile file, boolean overwrite) {
        // 模拟批量导入逻辑
        Map<String, Object> result = new HashMap<>();
        result.put("importedCount", 50);
        result.put("skippedCount", 5);
        result.put("errorCount", 2);
        result.put("overwrite", overwrite);
        result.put("importTime", LocalDateTime.now());
        
        List<String> errors = Arrays.asList(
            "第3行：歌手名称不能为空",
            "第7行：歌曲标题重复"
        );
        result.put("errors", errors);
        
        log.info("批量导入歌曲完成: 导入={}, 跳过={}, 错误={}", 50, 5, 2);
        
        return result;
    }
    
    @Override
    public String exportTracks(String format, String status, String category) {
        // 模拟导出逻辑，生成下载URL
        String fileName = "tracks_export_" + System.currentTimeMillis() + "." + format.toLowerCase();
        String downloadUrl = "https://cdn.example.com/exports/" + fileName;
        
        log.info("导出曲库数据: format={}, status={}, category={}, url={}", 
                format, status, category, downloadUrl);
        
        return downloadUrl;
    }
    
    /**
     * 检查是否有重要字段变更
     */
    private boolean hasImportantFieldChanged(Track existing, Track update) {
        return (update.getTitle() != null && !update.getTitle().equals(existing.getTitle())) ||
               (update.getArtist() != null && !update.getArtist().equals(existing.getArtist())) ||
               (update.getCoverUrl() != null && !update.getCoverUrl().equals(existing.getCoverUrl())) ||
               (update.getStatus() != null && !update.getStatus().equals(existing.getStatus()));
    }
    
    /**
     * 生成CDN URL
     */
    private String generateCdnUrl(Long trackId, String fileType, String originalFileName) {
        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        return String.format("https://cdn.example.com/media/%d/%s%s", 
                trackId, fileType.toLowerCase(), extension);
    }
    
    /**
     * 更新歌曲媒体信息
     */
    private void updateTrackMediaInfo(Track track, String fileType, String cdnUrl, long fileSize, String quality) {
        switch (fileType.toUpperCase()) {
            case "VIDEO":
                track.setLocalFilePath(null); // CDN URL不存储在local_file_path中
                track.setFileSize(fileSize);
                if (quality != null) {
                    track.setVideoQuality(quality);
                }
                break;
            case "AUDIO":
                track.setFileSize(fileSize);
                if (quality != null) {
                    track.setAudioQuality(quality);
                }
                break;
            case "COVER":
                track.setCoverUrl(cdnUrl);
                break;
            case "LYRICS":
                track.setLyricsUrl(cdnUrl);
                break;
        }
    }
}
