package com.boxai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.boxai.domain.entity.MediaSyncLog;
import com.boxai.domain.entity.Track;
import com.boxai.domain.mapper.MediaSyncLogMapper;
import com.boxai.domain.mapper.TrackMapper;
import com.boxai.service.MediaSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

/**
 * 媒体同步服务实现类
 * 负责管理本地媒体文件的同步和更新
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MediaSyncServiceImpl implements MediaSyncService {
    
    private final TrackMapper trackMapper;
    private final MediaSyncLogMapper mediaSyncLogMapper;
    
    /**
     * 获取同步历史记录
     */
    public List<MediaSyncLog> getSyncHistory(Long roomId, Integer limit) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MediaSyncLog> queryWrapper = 
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        if (roomId != null) {
            queryWrapper.eq("room_id", roomId);
        }
        queryWrapper.orderByDesc("started_at");
        if (limit != null && limit > 0) {
            queryWrapper.last("LIMIT " + limit);
        }
        
        return mediaSyncLogMapper.selectList(queryWrapper);
    }
    
    private static final String LOCAL_MEDIA_PATH = "/opt/boxai/media/"; // 本地媒体存储路径
    
    @Override
    @Transactional
    public boolean syncTrackToLocal(Long trackId, Long roomId) {
        Track track = trackMapper.selectById(trackId);
        if (track == null) {
            log.warn("歌曲不存在: trackId={}", trackId);
            return false;
        }
        
        // 记录同步开始
        MediaSyncLog syncLog = new MediaSyncLog();
        syncLog.setTrackId(trackId);
        syncLog.setRoomId(roomId);
        syncLog.setSyncType("DOWNLOAD");
        syncLog.setSyncStatus("PENDING");
        syncLog.setStartedAt(LocalDateTime.now());
        mediaSyncLogMapper.insert(syncLog);
        
        try {
            // 这里应该实现实际的文件下载逻辑
            // 从云端下载媒体文件到本地
            String localPath = LOCAL_MEDIA_PATH + trackId + ".mp4";
            
            // 模拟下载过程（实际应该从云端下载）
            // downloadFromCloud(track.getCoverUrl(), localPath);
            
            // 更新歌曲的本地文件信息
            track.setLocalFilePath(localPath);
            track.setLastSyncAt(LocalDateTime.now());
            trackMapper.updateById(track);
            
            // 更新同步状态为成功
            syncLog.setSyncStatus("SUCCESS");
            syncLog.setFilePath(localPath);
            syncLog.setCompletedAt(LocalDateTime.now());
            mediaSyncLogMapper.updateById(syncLog);
            
            log.info("歌曲同步成功: trackId={}, localPath={}", trackId, localPath);
            return true;
            
        } catch (Exception e) {
            // 更新同步状态为失败
            syncLog.setSyncStatus("FAILED");
            syncLog.setErrorMessage(e.getMessage());
            syncLog.setCompletedAt(LocalDateTime.now());
            mediaSyncLogMapper.updateById(syncLog);
            
            log.error("歌曲同步失败: trackId={}", trackId, e);
            return false;
        }
    }
    
    @Override
    public int batchSyncTracksToLocal(List<Long> trackIds, Long roomId) {
        int successCount = 0;
        for (Long trackId : trackIds) {
            if (syncTrackToLocal(trackId, roomId)) {
                successCount++;
            }
        }
        return successCount;
    }
    
    @Override
    public boolean isLocalFileExists(Long trackId) {
        Track track = trackMapper.selectById(trackId);
        if (track == null || track.getLocalFilePath() == null) {
            return false;
        }
        
        File file = new File(track.getLocalFilePath());
        return file.exists() && file.isFile();
    }
    
    @Override
    public String getLocalFilePath(Long trackId) {
        Track track = trackMapper.selectById(trackId);
        return track != null ? track.getLocalFilePath() : null;
    }
    
    @Override
    public boolean deleteLocalFile(Long trackId) {
        Track track = trackMapper.selectById(trackId);
        if (track == null || track.getLocalFilePath() == null) {
            return false;
        }
        
        File file = new File(track.getLocalFilePath());
        boolean deleted = file.delete();
        
        if (deleted) {
            // 清空数据库中的本地文件路径
            track.setLocalFilePath(null);
            track.setFileSize(null);
            trackMapper.updateById(track);
            
            // 记录删除日志
            MediaSyncLog syncLog = new MediaSyncLog();
            syncLog.setTrackId(trackId);
            syncLog.setSyncType("DELETE");
            syncLog.setSyncStatus("SUCCESS");
            syncLog.setFilePath(track.getLocalFilePath());
            syncLog.setStartedAt(LocalDateTime.now());
            syncLog.setCompletedAt(LocalDateTime.now());
            mediaSyncLogMapper.insert(syncLog);
            
            log.info("本地文件删除成功: trackId={}", trackId);
        }
        
        return deleted;
    }
    
    @Override
    public int syncUpdatedTracks(Long roomId) {
        // 获取需要更新的歌曲（本地版本低于云端版本）
        QueryWrapper<Track> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", "ACTIVE")
                   .isNotNull("sync_version")
                   .and(wrapper -> wrapper.isNull("local_file_path")
                                         .or()
                                         .isNull("last_sync_at"));
        
        List<Track> tracksToUpdate = trackMapper.selectList(queryWrapper);
        
        int successCount = 0;
        for (Track track : tracksToUpdate) {
            if (syncTrackToLocal(track.getId(), roomId)) {
                successCount++;
            }
        }
        
        log.info("批量同步更新完成: 成功={}, 总数={}", successCount, tracksToUpdate.size());
        return successCount;
    }
    
    @Override
    public StorageInfo getStorageInfo() {
        StorageInfo info = new StorageInfo();
        
        try {
            Path mediaPath = Paths.get(LOCAL_MEDIA_PATH);
            if (Files.exists(mediaPath)) {
                // 计算存储空间信息
                long totalSpace = Files.getFileStore(mediaPath).getTotalSpace();
                long usableSpace = Files.getFileStore(mediaPath).getUsableSpace();
                long usedSpace = totalSpace - usableSpace;
                
                info.setTotalSpace(totalSpace);
                info.setUsedSpace(usedSpace);
                info.setFreeSpace(usableSpace);
                
                // 计算本地歌曲数量
                QueryWrapper<Track> queryWrapper = new QueryWrapper<>();
                queryWrapper.isNotNull("local_file_path");
                int trackCount = Math.toIntExact(trackMapper.selectCount(queryWrapper));
                info.setTrackCount(trackCount);
            }
        } catch (IOException e) {
            log.error("获取存储信息失败", e);
        }
        
        return info;
    }
    
    @Override
    public int cleanupUnusedFiles() {
        int cleanedCount = 0;
        
        try {
            Path mediaPath = Paths.get(LOCAL_MEDIA_PATH);
            if (!Files.exists(mediaPath)) {
                return 0;
            }
            
            // 获取所有本地文件
            try (Stream<Path> files = Files.list(mediaPath)) {
                for (Path file : files.toArray(Path[]::new)) {
                    String fileName = file.getFileName().toString();
                    
                    // 检查文件是否在数据库中存在
                    QueryWrapper<Track> queryWrapper = new QueryWrapper<>();
                    queryWrapper.like("local_file_path", fileName);
                    
                    if (trackMapper.selectCount(queryWrapper) == 0) {
                        // 文件在数据库中不存在，删除
                        Files.delete(file);
                        cleanedCount++;
                        log.info("清理未使用文件: {}", fileName);
                    }
                }
            }
        } catch (IOException e) {
            log.error("清理未使用文件失败", e);
        }
        
        log.info("清理完成，删除文件数量: {}", cleanedCount);
        return cleanedCount;
    }
}
