package com.boxai.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建播放列表请求参数
 */
@Data
@Schema(description = "创建播放列表请求")
public class PlaylistCreateRequest {
    /**
     * 播放列表名称
     */
    @NotBlank
    @Schema(description = "播放列表名称", example = "我的收藏", required = true)
    private String name;
}
