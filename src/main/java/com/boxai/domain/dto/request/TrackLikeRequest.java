package com.boxai.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 曲目点赞请求参数
 */
@Data
@Schema(description = "曲目点赞请求")
public class TrackLikeRequest {
    /**
     * 曲目ID
     */
    @Schema(description = "曲目ID", example = "123", required = true)
    private Long trackId;
    
    /**
     * 用户ID
     */
    @Schema(description = "用户ID", example = "456", required = true)
    private Long userId;
}
