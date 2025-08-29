package com.boxai.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 播放控制请求参数
 */
@Data
@Schema(description = "播放控制请求")
public class PlaybackControlRequest {
    /**
     * 房间ID
     */
    @Schema(description = "房间ID", example = "1", required = true)
    private Long roomId;
    
    /**
     * 控制动作
     */
    @Schema(description = "控制动作", example = "PLAY", 
            allowableValues = {"PLAY", "PAUSE", "NEXT", "PREVIOUS", "STOP"}, 
            required = true)
    private String action;
}
