package com.boxai.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户偏好标签实体类
 * 基于用户行为分析生成的偏好标签
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_user_preference")
@Schema(description = "用户偏好标签")
public class UserPreference extends BaseEntity {
    /**
     * 用户偏好ID
     */
    @TableId
    @Schema(description = "用户偏好ID", example = "1")
    private Long id;
    
    /**
     * 用户ID
     */
    @Schema(description = "用户ID", example = "1001", required = true)
    private Long userId;
    
    /**
     * 偏好类型
     * CATEGORY: 分类偏好（流行、摇滚等）
     * ARTIST: 歌手偏好
     * GENRE: 风格偏好（抗情、快歌等）
     * LANGUAGE: 语言偏好（中文、英文等）
     */
    @Schema(description = "偏好类型", example = "CATEGORY")
    private String preferenceType;
    
    /**
     * 偏好值
     * 具体的偏好内容，如“流行”、“郓丽君”等
     */
    @Schema(description = "偏好值", example = "流行")
    private String preferenceValue;
    
    /**
     * 偏好分数
     * 根据用户行为计算得出，分数越高表示偏好程度越强
     */
    @Schema(description = "偏好分数", example = "0.85")
    private Double score;
}
