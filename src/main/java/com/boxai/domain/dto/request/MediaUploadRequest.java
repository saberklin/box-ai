package com.boxai.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 媒体文件上传请求DTO
 */
@Data
@Schema(description = "媒体文件上传请求")
public class MediaUploadRequest {
    
    @NotNull(message = "歌曲ID不能为空")
    @Schema(description = "歌曲ID", example = "1001", required = true)
    private Long trackId;
    
    @NotBlank(message = "文件类型不能为空")
    @Schema(description = "文件类型", example = "VIDEO", allowableValues = {"VIDEO", "AUDIO", "COVER", "LYRICS"}, required = true)
    private String fileType;
    
    @NotBlank(message = "文件名不能为空")
    @Schema(description = "原始文件名", example = "告白气球.mp4", required = true)
    private String originalFileName;
    
    @Schema(description = "文件大小（字节）", example = "52428800")
    private Long fileSize;
    
    @Schema(description = "文件MD5校验码", example = "d41d8cd98f00b204e9800998ecf8427e")
    private String fileMd5;
    
    @Schema(description = "文件质量标识", example = "1080P")
    private String quality;
    
    @Schema(description = "上传备注", example = "高清版本")
    private String remark;
}
