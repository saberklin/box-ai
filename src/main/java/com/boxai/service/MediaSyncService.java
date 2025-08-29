package com.boxai.service;

import com.boxai.domain.entity.Track;
import com.boxai.domain.entity.MediaSyncLog;

import java.util.List;

/**
 * 媒体文件同步服务接口
 * 负责云端到本地的媒体文件同步
 */
public interface MediaSyncService {
    
    /**
     * 同步单个歌曲文件到本地
     * 
     * @param trackId 歌曲ID
     * @param roomId 包间ID（可为null，表示全局同步）
     * @return 是否同步成功
     */
    boolean syncTrackToLocal(Long trackId, Long roomId);
    
    /**
     * 批量同步歌曲文件到本地
     * 
     * @param trackIds 歌曲ID列表
     * @param roomId 包间ID（可为null）
     * @return 同步成功的歌曲数量
     */
    int batchSyncTracksToLocal(List<Long> trackIds, Long roomId);
    
    /**
     * 检查本地文件是否存在
     * 
     * @param trackId 歌曲ID
     * @return 是否存在
     */
    boolean isLocalFileExists(Long trackId);
    
    /**
     * 获取本地文件路径
     * 
     * @param trackId 歌曲ID
     * @return 本地文件路径
     */
    String getLocalFilePath(Long trackId);
    
    /**
     * 删除本地文件
     * 
     * @param trackId 歌曲ID
     * @return 是否删除成功
     */
    boolean deleteLocalFile(Long trackId);
    
    /**
     * 检查并同步更新的歌曲
     * 比较本地和云端的版本号，同步更新的歌曲
     * 
     * @param roomId 包间ID（可为null）
     * @return 同步更新的歌曲数量
     */
    int syncUpdatedTracks(Long roomId);
    
    /**
     * 获取本地存储空间信息
     * 
     * @return 存储空间信息（字节）
     */
    StorageInfo getStorageInfo();
    
    /**
     * 清理未使用的本地文件
     * 删除不在数据库中或已被标记为删除的本地文件
     * 
     * @return 清理的文件数量
     */
    int cleanupUnusedFiles();
    
    /**
     * 获取同步历史记录
     * 
     * @param roomId 包间ID（可为null）
     * @param limit 限制数量
     * @return 同步历史记录列表
     */
    List<MediaSyncLog> getSyncHistory(Long roomId, Integer limit);
    
    /**
     * 存储空间信息
     */
    class StorageInfo {
        private long totalSpace;     // 总空间
        private long usedSpace;      // 已使用空间
        private long freeSpace;      // 可用空间
        private int trackCount;      // 本地歌曲数量
        
        // Getters and Setters
        public long getTotalSpace() { return totalSpace; }
        public void setTotalSpace(long totalSpace) { this.totalSpace = totalSpace; }
        
        public long getUsedSpace() { return usedSpace; }
        public void setUsedSpace(long usedSpace) { this.usedSpace = usedSpace; }
        
        public long getFreeSpace() { return freeSpace; }
        public void setFreeSpace(long freeSpace) { this.freeSpace = freeSpace; }
        
        public int getTrackCount() { return trackCount; }
        public void setTrackCount(int trackCount) { this.trackCount = trackCount; }
    }
}
