package com.boxai.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 本地媒体文件同步记录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_media_sync_log")
@Schema(description = "本地媒体文件同步记录")
public class MediaSyncLog extends BaseEntity {
    
    @Schema(description = "包间ID（null表示全局同步）")
    private Long roomId;
    
    @Schema(description = "曲目ID")
    private Long trackId;
    
    @Schema(description = "同步类型：DOWNLOAD/UPDATE/DELETE")
    private String syncType;
    
    @Schema(description = "本地文件路径")
    private String filePath;
    
    @Schema(description = "文件大小")
    private Long fileSize;
    
    @Schema(description = "同步状态：PENDING/SUCCESS/FAILED")
    private String syncStatus;
    
    @Schema(description = "错误信息")
    private String errorMessage;
    
    @Schema(description = "同步开始时间")
    private LocalDateTime startedAt;
    
    @Schema(description = "同步完成时间")
    private LocalDateTime completedAt;
}
