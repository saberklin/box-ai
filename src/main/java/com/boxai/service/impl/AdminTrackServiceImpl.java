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

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
        String uploadId = "upload_" + System.currentTimeMillis();
        
        try {
            // 1. 验证文件格式和大小
            validateMediaFile(file, request.getFileType());
            
            // 2. 验证歌曲是否存在
            Track track = trackMapper.selectById(request.getTrackId());
            if (track == null) {
                throw new RuntimeException("歌曲不存在: " + request.getTrackId());
            }
            
            // 3. 计算文件MD5（如果没有提供）
            String fileMd5 = request.getFileMd5();
            if (fileMd5 == null || fileMd5.isEmpty()) {
                fileMd5 = calculateFileMd5(file);
            }
            
            // 4. 实际上传文件到CDN/对象存储
            String cdnUrl = uploadToStorage(file, request.getTrackId(), request.getFileType());
            
            // 5. 根据文件类型更新相应字段
            updateTrackMediaInfo(track, request.getFileType(), cdnUrl, file.getSize(), request.getQuality());
            
            // 6. 更新同步版本号
            Long currentVersion = track.getSyncVersion();
            track.setSyncVersion(currentVersion != null ? currentVersion + 1 : 1L);
            track.setLastSyncAt(LocalDateTime.now());
            trackMapper.updateById(track);
            
            // 7. 记录上传日志
            recordUploadLog(request.getTrackId(), request.getFileType(), cdnUrl, file.getSize(), "SUCCESS");
            
            MediaUploadResponse response = new MediaUploadResponse();
            response.setUploadId(uploadId);
            response.setCdnUrl(cdnUrl);
            response.setFileSize(file.getSize());
            response.setUploadStatus("SUCCESS");
            response.setMessage("文件上传成功");
            response.setFileMd5(fileMd5);
            
            log.info("媒体文件上传成功: trackId={}, fileType={}, cdnUrl={}, size={}", 
                    request.getTrackId(), request.getFileType(), cdnUrl, file.getSize());
            
            return response;
            
        } catch (Exception e) {
            log.error("媒体文件上传失败: trackId={}, fileType={}", request.getTrackId(), request.getFileType(), e);
            
            // 记录失败日志
            recordUploadLog(request.getTrackId(), request.getFileType(), null, file.getSize(), "FAILED: " + e.getMessage());
            
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
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int failedCount = 0;
        List<String> errors = new ArrayList<>();
        
        try {
            // 验证歌曲是否存在
            List<Track> tracks = trackMapper.selectBatchIds(trackIds);
            if (tracks.size() != trackIds.size()) {
                List<Long> existingIds = tracks.stream().map(Track::getId).collect(Collectors.toList());
                List<Long> missingIds = trackIds.stream()
                        .filter(id -> !existingIds.contains(id))
                        .collect(Collectors.toList());
                errors.add("以下歌曲不存在: " + missingIds);
            }
            
            // 更新歌曲的sync_version，触发同步
            for (Track track : tracks) {
                try {
                    // 生成新的同步版本号
                    Long currentVersion = track.getSyncVersion();
                    track.setSyncVersion(currentVersion != null ? currentVersion + 1 : 1L);
                    track.setLastSyncAt(LocalDateTime.now());
                    trackMapper.updateById(track);
                    successCount++;
                    
                    log.info("强制推送歌曲更新: trackId={}, newVersion={}", track.getId(), track.getSyncVersion());
                } catch (Exception e) {
                    failedCount++;
                    errors.add("歌曲ID " + track.getId() + " 推送失败: " + e.getMessage());
                    log.error("推送歌曲更新失败: trackId={}", track.getId(), e);
                }
            }
            
            result.put("pushedTrackCount", successCount);
            result.put("failedTrackCount", failedCount);
            result.put("targetRoomCount", roomIds != null ? roomIds.size() : "ALL");
            result.put("pushTime", LocalDateTime.now());
            result.put("status", failedCount == 0 ? "SUCCESS" : "PARTIAL_SUCCESS");
            result.put("errors", errors);
            
            log.info("强制推送更新完成: 成功={}, 失败={}, 目标房间={}", 
                    successCount, failedCount, roomIds != null ? roomIds.size() : "ALL");
            
        } catch (Exception e) {
            result.put("status", "FAILED");
            result.put("error", e.getMessage());
            log.error("强制推送更新失败", e);
        }
        
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
        Map<String, Object> result = new HashMap<>();
        int importedCount = 0;
        int skippedCount = 0;
        int errorCount = 0;
        List<String> errors = new ArrayList<>();
        
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("导入文件不能为空");
            }
            
            // 检查文件类型
            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.endsWith(".csv") && !filename.endsWith(".xlsx"))) {
                throw new RuntimeException("只支持CSV和Excel文件格式");
            }
            
            // 解析文件内容（这里简化处理，实际应该使用Apache POI或OpenCSV）
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            String[] lines = content.split("\n");
            
            // 跳过标题行
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;
                
                try {
                    String[] fields = line.split(",");
                    if (fields.length < 3) {
                        errorCount++;
                        errors.add("第" + (i + 1) + "行：字段不完整，至少需要标题、歌手、专辑");
                        continue;
                    }
                    
                    String title = fields[0].trim();
                    String artist = fields[1].trim();
                    String album = fields[2].trim();
                    
                    if (title.isEmpty() || artist.isEmpty()) {
                        errorCount++;
                        errors.add("第" + (i + 1) + "行：歌曲标题和歌手不能为空");
                        continue;
                    }
                    
                    // 检查是否已存在
                    QueryWrapper<Track> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("title", title).eq("artist", artist);
                    Track existingTrack = trackMapper.selectOne(queryWrapper);
                    
                    if (existingTrack != null) {
                        if (!overwrite) {
                            skippedCount++;
                            continue;
                        }
                        // 更新现有歌曲
                        existingTrack.setAlbum(album);
                        existingTrack.setUpdatedAt(LocalDateTime.now().atOffset(java.time.ZoneOffset.UTC));
                        trackMapper.updateById(existingTrack);
                        importedCount++;
                    } else {
                        // 创建新歌曲
                        Track newTrack = new Track();
                        newTrack.setTitle(title);
                        newTrack.setArtist(artist);
                        newTrack.setAlbum(album);
                        newTrack.setStatus("ACTIVE");
                        newTrack.setCreatedAt(LocalDateTime.now().atOffset(java.time.ZoneOffset.UTC));
                        newTrack.setUpdatedAt(LocalDateTime.now().atOffset(java.time.ZoneOffset.UTC));
                        trackMapper.insert(newTrack);
                        importedCount++;
                    }
                    
                } catch (Exception e) {
                    errorCount++;
                    errors.add("第" + (i + 1) + "行：处理失败 - " + e.getMessage());
                }
            }
            
            result.put("importedCount", importedCount);
            result.put("skippedCount", skippedCount);
            result.put("errorCount", errorCount);
            result.put("overwrite", overwrite);
            result.put("importTime", LocalDateTime.now());
            result.put("errors", errors);
            result.put("status", errorCount == 0 ? "SUCCESS" : "PARTIAL_SUCCESS");
            
            log.info("批量导入歌曲完成: 导入={}, 跳过={}, 错误={}", importedCount, skippedCount, errorCount);
            
        } catch (Exception e) {
            result.put("status", "FAILED");
            result.put("error", e.getMessage());
            log.error("批量导入歌曲失败", e);
        }
        
        return result;
    }
    
    @Override
    public String exportTracks(String format, String status, String category) {
        try {
            // 构建查询条件
            QueryWrapper<Track> queryWrapper = new QueryWrapper<>();
            if (status != null && !status.isEmpty()) {
                queryWrapper.eq("status", status);
            }
            if (category != null && !category.isEmpty()) {
                queryWrapper.eq("genre", category);
            }
            
            // 查询数据
            List<Track> tracks = trackMapper.selectList(queryWrapper);
            
            // 生成导出文件
            String fileName = "tracks_export_" + System.currentTimeMillis() + "." + format.toLowerCase();
            String exportPath = "/tmp/exports/" + fileName; // 实际部署时应该配置专门的导出目录
            
            if ("csv".equalsIgnoreCase(format)) {
                exportToCsv(tracks, exportPath);
            } else if ("excel".equalsIgnoreCase(format) || "xlsx".equalsIgnoreCase(format)) {
                exportToExcel(tracks, exportPath);
            } else {
                throw new RuntimeException("不支持的导出格式: " + format);
            }
            
            // 生成下载URL（实际部署时应该是真实的文件服务URL）
            String downloadUrl = "http://localhost:9998/api/admin/tracks/download/" + fileName;
            
            log.info("导出曲库数据完成: format={}, status={}, category={}, count={}, url={}", 
                    format, status, category, tracks.size(), downloadUrl);
            
            return downloadUrl;
            
        } catch (Exception e) {
            log.error("导出曲库数据失败: format={}, status={}, category={}", format, status, category, e);
            throw new RuntimeException("导出失败: " + e.getMessage());
        }
    }
    
    /**
     * 导出为CSV格式
     */
    private void exportToCsv(List<Track> tracks, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath, StandardCharsets.UTF_8)) {
            // 写入CSV头部
            writer.write("ID,标题,歌手,专辑,时长,类型,语言,发行年份,播放次数,点赞次数,状态,创建时间\n");
            
            // 写入数据行
            for (Track track : tracks) {
                writer.write(String.format("%d,%s,%s,%s,%d,%s,%s,%d,%d,%d,%s,%s\n",
                        track.getId(),
                        escapeCSV(track.getTitle()),
                        escapeCSV(track.getArtist()),
                        escapeCSV(track.getAlbum()),
                        track.getDuration() != null ? track.getDuration() : 0,
                        escapeCSV(track.getGenre()),
                        escapeCSV(track.getLanguage()),
                        0, // release year not available
                        track.getPlayCount() != null ? track.getPlayCount() : 0,
                        track.getLikeCount() != null ? track.getLikeCount() : 0,
                        escapeCSV(track.getStatus()),
                        track.getCreatedAt() != null ? track.getCreatedAt().toString() : ""
                ));
            }
        }
    }
    
    /**
     * 导出为Excel格式（简化实现）
     */
    private void exportToExcel(List<Track> tracks, String filePath) throws IOException {
        // 这里应该使用Apache POI来生成真正的Excel文件
        // 为了简化，暂时生成CSV格式但使用.xlsx扩展名
        exportToCsv(tracks, filePath);
    }
    
    /**
     * CSV字段转义
     */
    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
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
    
    /**
     * 验证媒体文件
     */
    private void validateMediaFile(MultipartFile file, String fileType) {
        if (file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new RuntimeException("文件名不能为空");
        }
        
        // 验证文件大小（100MB限制）
        long maxSize = 100 * 1024 * 1024; // 100MB
        if (file.getSize() > maxSize) {
            throw new RuntimeException("文件大小不能超过100MB");
        }
        
        // 验证文件格式
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        switch (fileType.toUpperCase()) {
            case "VIDEO":
                if (!Arrays.asList("mp4", "avi", "mkv", "mov", "wmv").contains(extension)) {
                    throw new RuntimeException("视频文件格式不支持，支持格式：mp4, avi, mkv, mov, wmv");
                }
                break;
            case "AUDIO":
                if (!Arrays.asList("mp3", "wav", "flac", "aac", "ogg").contains(extension)) {
                    throw new RuntimeException("音频文件格式不支持，支持格式：mp3, wav, flac, aac, ogg");
                }
                break;
            case "COVER":
                if (!Arrays.asList("jpg", "jpeg", "png", "gif", "webp").contains(extension)) {
                    throw new RuntimeException("图片文件格式不支持，支持格式：jpg, jpeg, png, gif, webp");
                }
                break;
            case "LYRICS":
                if (!Arrays.asList("lrc", "txt").contains(extension)) {
                    throw new RuntimeException("歌词文件格式不支持，支持格式：lrc, txt");
                }
                break;
            default:
                throw new RuntimeException("不支持的文件类型：" + fileType);
        }
    }
    
    /**
     * 计算文件MD5
     */
    private String calculateFileMd5(MultipartFile file) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(file.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("计算文件MD5失败", e);
            return "unknown";
        }
    }
    
    /**
     * 上传文件到存储服务
     */
    private String uploadToStorage(MultipartFile file, Long trackId, String fileType) throws IOException {
        // 生成存储路径
        String extension = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
        String fileName = String.format("%d_%s_%d%s", trackId, fileType.toLowerCase(), System.currentTimeMillis(), extension);
        
        // 实际部署时应该上传到CDN或对象存储（如阿里云OSS、腾讯云COS等）
        // 这里模拟本地存储，实际应该替换为真实的CDN上传逻辑
        String uploadDir = "/opt/boxai/uploads/" + fileType.toLowerCase() + "/";
        Path uploadPath = Paths.get(uploadDir);
        
        // 确保目录存在
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // 保存文件
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        // 返回CDN URL（实际部署时应该返回真实的CDN地址）
        String cdnUrl = String.format("https://cdn.boxai.com/media/%s/%s", fileType.toLowerCase(), fileName);
        
        log.info("文件上传到存储: localPath={}, cdnUrl={}", filePath, cdnUrl);
        return cdnUrl;
    }
    
    /**
     * 记录上传日志
     */
    private void recordUploadLog(Long trackId, String fileType, String cdnUrl, long fileSize, String status) {
        try {
            MediaSyncLog syncLog = new MediaSyncLog();
            syncLog.setTrackId(trackId);
            syncLog.setSyncType("UPLOAD_" + fileType.toUpperCase());
            syncLog.setSyncStatus(status.startsWith("SUCCESS") ? "SUCCESS" : "FAILED");
            syncLog.setFilePath(cdnUrl);
            syncLog.setFileSize(fileSize);
            syncLog.setStartedAt(LocalDateTime.now());
            syncLog.setCompletedAt(LocalDateTime.now());
            if (status.startsWith("FAILED")) {
                syncLog.setErrorMessage(status);
            }
            
            mediaSyncLogMapper.insert(syncLog);
        } catch (Exception e) {
            log.warn("记录上传日志失败", e);
        }
    }
}
