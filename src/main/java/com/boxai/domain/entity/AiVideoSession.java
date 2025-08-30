package com.boxai.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * AI视频生成会话实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_ai_video_session")
@Schema(description = "AI视频生成会话")
public class AiVideoSession extends BaseEntity {
    
    @Schema(description = "包间ID")
    private Long roomId;
    
    @Schema(description = "用户ID")
    private Long userId;
    
    @Schema(description = "流任务ID")
    private String streamId;
    
    @Schema(description = "视频类型")
    private String videoType;
    
    @Schema(description = "AI提示词")
    private String prompt;
    
    @Schema(description = "视频风格")
    private String style;
    
    @Schema(description = "视频分辨率")
    private String resolution;
    
    @Schema(description = "帧率")
    private Integer frameRate;
    
    @Schema(description = "视频时长（秒）")
    private Integer duration;
    
    @Schema(description = "音频同步")
    private Boolean audioSync;
    
    @Schema(description = "当前播放的歌曲ID")
    private Long currentTrackId;
    
    @Schema(description = "生成状态")
    private String status;
    
    @Schema(description = "生成进度")
    private Integer progress;
    
    @Schema(description = "流媒体URL")
    private String streamUrl;
    
    @Schema(description = "WebRTC流URL")
    private String webrtcUrl;
    
    @Schema(description = "HLS播放URL")
    private String hlsUrl;
    
    @Schema(description = "比特率")
    private Integer bitrate;
    
    @Schema(description = "开始时间")
    private LocalDateTime startTime;
    
    @Schema(description = "结束时间")
    private LocalDateTime endTime;
    
    @Schema(description = "错误信息")
    private String errorMessage;
    
    @Schema(description = "生成参数信息")
    private String generationInfo;
}
