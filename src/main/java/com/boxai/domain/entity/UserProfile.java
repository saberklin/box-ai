package com.boxai.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.Map;

/**
 * 用户画像实体类
 * 记录用户的偏好分析、使用频次等画像数据
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_user_profile")
@Schema(description = "用户画像信息")
public class UserProfile extends BaseEntity {
    /**
     * 用户画像主键ID
     */
    @TableId
    @Schema(description = "用户画像主键ID", example = "1")
    private Long id;
    
    /**
     * 用户ID
     * 关联到t_user表的id字段
     */
    @Schema(description = "用户ID", example = "123", required = true)
    private Long userId;
    
    /**
     * 偏好歌曲类型统计
     * JSON格式存储，如：{"流行": 25, "摇滚": 15, "民谣": 10}
     */
    @Schema(description = "偏好歌曲类型统计(JSON格式)", example = "{\"流行\": 25, \"摇滚\": 15, \"民谣\": 10}")
    private String favoriteCategories;
    
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
     * 偏好时间段
     * JSON格式存储，如：{"morning": 5, "afternoon": 10, "evening": 20, "night": 8}
     */
    @Schema(description = "偏好时间段(JSON格式)", example = "{\"morning\": 5, \"afternoon\": 10, \"evening\": 20, \"night\": 8}")
    private String preferredTimeSlots;
}
