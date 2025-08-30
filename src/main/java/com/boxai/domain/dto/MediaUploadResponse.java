package com.boxai.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 媒体文件上传响应DTO
 */
@Data
@Schema(description = "媒体文件上传响应")
public class MediaUploadResponse {
    
    @Schema(description = "上传任务ID", example = "upload_12345")
    private String uploadId;
    
    @Schema(description = "CDN文件URL", example = "https://cdn.example.com/media/1001/video.mp4")
    private String cdnUrl;
    
    @Schema(description = "文件大小", example = "52428800")
    private Long fileSize;
    
    @Schema(description = "上传状态", example = "SUCCESS", allowableValues = {"SUCCESS", "FAILED", "PROCESSING"})
    private String uploadStatus;
    
    @Schema(description = "处理消息", example = "文件上传成功")
    private String message;
    
    @Schema(description = "文件MD5校验码", example = "d41d8cd98f00b204e9800998ecf8427e")
    private String fileMd5;
}
