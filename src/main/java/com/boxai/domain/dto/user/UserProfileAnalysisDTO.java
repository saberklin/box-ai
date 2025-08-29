package com.boxai.domain.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

/**
 * 用户画像分析DTO
 * 包含用户的偏好分析和使用频次统计
 */
@Data
@Schema(description = "用户画像分析结果")
public class UserProfileAnalysisDTO {
    /**
     * 用户ID
     */
    @Schema(description = "用户ID", example = "123")
    private Long userId;
    
    /**
     * 用户昵称
     */
    @Schema(description = "用户昵称", example = "音乐爱好者")
    private String nickname;
    
    /**
     * 偏好歌曲类型统计
     * key: 类型名称, value: 播放次数
     */
    @Schema(description = "偏好歌曲类型统计", example = "{\"流行\": 25, \"摇滚\": 15}")
    private Map<String, Integer> favoriteCategories;
    
    /**
     * 使用频次统计
     */
    @Schema(description = "使用频次统计")
    private UsageFrequency usageFrequency;
    
    /**
     * 偏好时间段统计
     * key: 时间段, value: 活跃次数
     */
    @Schema(description = "偏好时间段统计", example = "{\"morning\": 5, \"evening\": 20}")
    private Map<String, Integer> preferredTimeSlots;
    
    /**
     * 最后活跃日期
     */
    @Schema(description = "最后活跃日期", example = "2024-01-15")
    private LocalDate lastActiveDate;
    
    /**
     * 平均会话时长（分钟）
     */
    @Schema(description = "平均会话时长(分钟)", example = "30")
    private Integer avgSessionDuration;
    
    /**
     * 用户标签
     */
    @Schema(description = "智能用户标签")
    private UserTags userTags;
    
    @Data
    @Schema(description = "使用频次统计")
    public static class UsageFrequency {
        /**
         * 总播放次数
         */
        @Schema(description = "总播放次数", example = "150")
        private Integer totalPlayCount;
        
        /**
         * 总点赞次数
         */
        @Schema(description = "总点赞次数", example = "45")
        private Integer totalLikeCount;
        
        /**
         * 总搜索次数
         */
        @Schema(description = "总搜索次数", example = "30")
        private Integer totalSearchCount;
        
        /**
         * 活跃天数
         */
        @Schema(description = "活跃天数", example = "15")
        private Integer activeDays;
        
        /**
         * 日均播放次数
         */
        @Schema(description = "日均播放次数", example = "10.0")
        private Double avgDailyPlays;
        
        /**
         * 活跃度等级
         * LOW-低活跃, MEDIUM-中等活跃, HIGH-高活跃, SUPER-超级活跃
         */
        @Schema(description = "活跃度等级", example = "HIGH", allowableValues = {"LOW", "MEDIUM", "HIGH", "SUPER"})
        private String activityLevel;
    }
    
    @Data
    @Schema(description = "用户标签")
    public static class UserTags {
        /**
         * 音乐偏好标签
         */
        @Schema(description = "音乐偏好标签", example = "流行爱好者")
        private String musicPreference;
        
        /**
         * 活跃度标签
         */
        @Schema(description = "活跃度标签", example = "高活跃用户")
        private String activityTag;
        
        /**
         * 使用习惯标签
         */
        @Schema(description = "使用习惯标签", example = "互动积极型")
        private String usageHabit;
        
        /**
         * 时间偏好标签
         */
        @Schema(description = "时间偏好标签", example = "黄昏音乐家")
        private String timePreference;
    }
}
