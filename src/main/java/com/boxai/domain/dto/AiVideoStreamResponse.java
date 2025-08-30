package com.boxai.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI视频流响应DTO
 */
@Data
@Schema(description = "AI视频流响应")
public class AiVideoStreamResponse {
    
    @Schema(description = "流任务ID", example = "ai_stream_12345")
    private String streamId;
    
    @Schema(description = "流媒体URL", example = "rtmp://localhost:1935/live/ai_stream_12345")
    private String streamUrl;
    
    @Schema(description = "WebRTC流URL", example = "webrtc://localhost:8080/stream/ai_stream_12345")
    private String webrtcUrl;
    
    @Schema(description = "HLS播放URL", example = "http://localhost:8080/hls/ai_stream_12345.m3u8")
    private String hlsUrl;
    
    @Schema(description = "生成状态", example = "GENERATING", 
            allowableValues = {"PENDING", "GENERATING", "STREAMING", "COMPLETED", "FAILED"})
    private String status;
    
    @Schema(description = "生成进度", example = "65")
    private Integer progress;
    
    @Schema(description = "预计剩余时间（秒）", example = "30")
    private Integer estimatedTimeRemaining;
    
    @Schema(description = "视频分辨率", example = "1920x1080")
    private String resolution;
    
    @Schema(description = "帧率", example = "30")
    private Integer frameRate;
    
    @Schema(description = "比特率", example = "5000")
    private Integer bitrate;
    
    @Schema(description = "开始时间")
    private LocalDateTime startTime;
    
    @Schema(description = "预计结束时间")
    private LocalDateTime estimatedEndTime;
    
    @Schema(description = "错误信息")
    private String errorMessage;
    
    @Schema(description = "生成参数信息")
    private String generationInfo;
}
