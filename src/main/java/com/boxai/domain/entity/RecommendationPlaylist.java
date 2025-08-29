package com.boxai.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 推荐歌单实体类
 * 系统生成的推荐歌单，如热门榜单、新歌榜单等
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_recommendation_playlist")
@Schema(description = "推荐歌单")
public class RecommendationPlaylist extends BaseEntity {
    /**
     * 推荐歌单ID
     */
    @TableId
    @Schema(description = "推荐歌单ID", example = "1")
    private Long id;
    
    /**
     * 推荐歌单名称
     */
    @Schema(description = "推荐歌单名称", example = "本周热门", required = true)
    private String name;
    
    /**
     * 歌单描述
     */
    @Schema(description = "歌单描述", example = "本周最受欢迎的歌曲合集")
    private String description;
    
    /**
     * 推荐类型
     * HOT: 热门榜单
     * NEW: 新歌榜单
     * CLASSIC: 经典榜单
     * GENRE_BASED: 基于风格的推荐
     */
    @Schema(description = "推荐类型", example = "HOT")
    private String type;
    
    /**
     * 目标受众
     * ALL: 所有用户
     * YOUNG: 年轻用户
     * MIDDLE_AGE: 中年用户
     */
    @Schema(description = "目标受众", example = "ALL")
    private String targetAudience;
    
    /**
     * 歌单封面URL
     */
    @Schema(description = "歌单封面URL", example = "https://cdn.boxai.com/playlist/hot_weekly.jpg")
    private String coverUrl;
    
    /**
     * 播放次数
     */
    @Schema(description = "播放次数", example = "12580")
    private Long playCount;
    
    /**
     * 是否启用
     */
    @Schema(description = "是否启用", example = "true")
    private Boolean isActive;
    
    /**
     * 排序权重
     * 数值越大排序越靠前
     */
    @Schema(description = "排序权重", example = "100")
    private Integer sortOrder;
}
