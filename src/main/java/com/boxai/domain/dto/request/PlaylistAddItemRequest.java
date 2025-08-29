package com.boxai.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 播放列表添加曲目请求参数
 */
@Data
@Schema(description = "播放列表添加曲目请求")
public class PlaylistAddItemRequest {
    /**
     * 曲目ID
     */
    @Schema(description = "曲目ID", example = "123", required = true)
    private Long trackId;
    
    /**
     * 插入位置
     */
    @Schema(description = "插入位置(0表示末尾)", example = "0")
    private Integer position;
}
