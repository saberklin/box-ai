package com.boxai.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 播放队列添加请求参数
 */
@Data
@Schema(description = "播放队列添加请求")
public class PlaybackQueueRequest {
    /**
     * 房间ID
     */
    @Schema(description = "房间ID", example = "1", required = true)
    private Long roomId;
    
    /**
     * 曲目ID
     */
    @Schema(description = "曲目ID", example = "123", required = true)
    private Long trackId;
    
    /**
     * 用户ID
     */
    @Schema(description = "用户ID", example = "456")
    private Long userId;
}
