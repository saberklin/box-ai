package com.boxai.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 推荐歌单曲目实体类
 * 推荐歌单中包含的具体曲目
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_recommendation_playlist_item")
@Schema(description = "推荐歌单曲目")
public class RecommendationPlaylistItem extends BaseEntity {
    /**
     * 推荐歌单曲目ID
     */
    @TableId
    @Schema(description = "推荐歌单曲目ID", example = "1")
    private Long id;
    
    /**
     * 歌单ID
     */
    @Schema(description = "歌单ID", example = "1", required = true)
    private Long playlistId;
    
    /**
     * 曲目ID
     */
    @Schema(description = "曲目ID", example = "1001", required = true)
    private Long trackId;
    
    /**
     * 在歌单中的位置
     */
    @Schema(description = "在歌单中的位置", example = "1")
    private Integer position;
    
    /**
     * 推荐权重
     * 用于算法排序，数值越大推荐优先级越高
     */
    @Schema(description = "推荐权重", example = "1.0")
    private Double weight;
}
