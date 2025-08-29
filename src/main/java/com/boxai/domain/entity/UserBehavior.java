package com.boxai.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 用户行为记录实体类
 * 记录用户的各种行为，用于计算用户画像
 */
@Data
@TableName("t_user_behavior")
@Schema(description = "用户行为记录")
public class UserBehavior {
    /**
     * 行为记录主键ID
     */
    @TableId
    @Schema(description = "行为记录主键ID", example = "1")
    private Long id;
    
    /**
     * 用户ID
     * 关联到t_user表的id字段
     */
    @Schema(description = "用户ID", example = "123", required = true)
    private Long userId;
    
    /**
     * 行为类型
     * PLAY-播放, LIKE-点赞, SEARCH-搜索, LOGIN-登录
     */
    @Schema(description = "行为类型", example = "PLAY", allowableValues = {"PLAY", "LIKE", "SEARCH", "LOGIN"})
    private String behaviorType;
    
    /**
     * 目标ID
     * 关联的目标ID（曲目ID、搜索词等）
     */
    @Schema(description = "目标ID(曲目ID等)", example = "456")
    private Long targetId;
    
    /**
     * 目标类型
     * TRACK-曲目, SEARCH_KEYWORD-搜索关键词
     */
    @Schema(description = "目标类型", example = "TRACK", allowableValues = {"TRACK", "SEARCH_KEYWORD"})
    private String targetType;
    
    /**
     * 元数据
     * JSON格式存储额外信息，如：{"category": "流行", "duration": 180}
     */
    @Schema(description = "元数据(JSON格式)", example = "{\"category\": \"流行\", \"duration\": 180}")
    private String metadata;
    
    /**
     * 会话ID
     * 用于标识同一次使用会话
     */
    @Schema(description = "会话ID", example = "session_123")
    private String sessionId;
    
    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2024-01-15T10:30:00Z")
    private OffsetDateTime createdAt;
    
    /**
     * 行为类型常量
     */
    public static class BehaviorType {
        public static final String PLAY = "PLAY";
        public static final String LIKE = "LIKE";
        public static final String SEARCH = "SEARCH";
        public static final String LOGIN = "LOGIN";
    }
    
    /**
     * 目标类型常量
     */
    public static class TargetType {
        public static final String TRACK = "TRACK";
        public static final String SEARCH_KEYWORD = "SEARCH_KEYWORD";
    }
}
