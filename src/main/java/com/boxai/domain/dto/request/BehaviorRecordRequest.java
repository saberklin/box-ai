package com.boxai.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户行为记录请求参数
 */
@Data
@Schema(description = "用户行为记录请求")
public class BehaviorRecordRequest {
    /**
     * 行为类型
     * PLAY-播放, LIKE-点赞, SEARCH-搜索, LOGIN-登录
     */
    @Schema(description = "行为类型", example = "PLAY", 
            allowableValues = {"PLAY", "LIKE", "SEARCH", "LOGIN"}, 
            required = true)
    private String behaviorType;
    
    /**
     * 目标ID
     */
    @Schema(description = "目标ID(曲目ID等)", example = "456")
    private Long targetId;
    
    /**
     * 目标类型
     * TRACK-曲目, SEARCH_KEYWORD-搜索关键词
     */
    @Schema(description = "目标类型", example = "TRACK", 
            allowableValues = {"TRACK", "SEARCH_KEYWORD"})
    private String targetType;
    
    /**
     * 元数据JSON字符串
     */
    @Schema(description = "元数据(JSON格式)", 
            example = "{\"category\": \"流行\", \"duration\": 180}")
    private String metadata;
    
    /**
     * 会话ID
     */
    @Schema(description = "会话ID", example = "session_123")
    private String sessionId;
}
