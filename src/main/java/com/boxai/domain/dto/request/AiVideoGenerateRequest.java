package com.boxai.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * AI视频生成请求DTO
 */
@Data
@Schema(description = "AI视频生成请求")
public class AiVideoGenerateRequest {
    
    @NotNull(message = "包间ID不能为空")
    @Schema(description = "包间ID", example = "1001", required = true)
    private Long roomId;
    
    @NotBlank(message = "视频类型不能为空")
    @Schema(description = "视频类型", example = "MUSIC_VISUALIZATION", 
            allowableValues = {"MUSIC_VISUALIZATION", "AMBIENT_SCENE", "DANCE_EFFECT", "CUSTOM_THEME"}, 
            required = true)
    private String videoType;
    
    @Schema(description = "AI提示词", example = "科幻未来城市，霓虹灯闪烁，配合音乐节拍")
    private String prompt;
    
    @Schema(description = "视频风格", example = "CYBERPUNK", 
            allowableValues = {"CYBERPUNK", "NATURE", "ABSTRACT", "RETRO", "MODERN"})
    private String style = "MODERN";
    
    @Schema(description = "视频分辨率", example = "1920x1080")
    private String resolution = "1920x1080";
    
    @Schema(description = "帧率", example = "30")
    private Integer frameRate = 30;
    
    @Schema(description = "视频时长（秒）", example = "60")
    private Integer duration = 60;
    
    @Schema(description = "音频同步", example = "true")
    private Boolean audioSync = false;
    
    @Schema(description = "当前播放的歌曲ID（用于音频同步）", example = "12345")
    private Long currentTrackId;
    
    @Schema(description = "生成优先级", example = "HIGH", allowableValues = {"LOW", "NORMAL", "HIGH"})
    private String priority = "NORMAL";
    
    @Schema(description = "是否实时生成", example = "true")
    private Boolean realtime = true;
}
