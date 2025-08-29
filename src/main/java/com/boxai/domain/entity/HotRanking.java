package com.boxai.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 热门榜单实体类
 * 定时计算生成的热门榜单数据
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_hot_ranking")
@Schema(description = "热门榜单")
public class HotRanking extends BaseEntity {
    /**
     * 热门榜单ID
     */
    @TableId
    @Schema(description = "热门榜单ID", example = "1")
    private Long id;
    
    /**
     * 榜单类型
     * DAILY: 日榜
     * WEEKLY: 周榜
     * MONTHLY: 月榜
     * CATEGORY: 分类榜单
     */
    @Schema(description = "榜单类型", example = "WEEKLY")
    private String rankingType;
    
    /**
     * 分类
     * 如果是分类榜单，指定具体分类
     */
    @Schema(description = "分类", example = "流行")
    private String category;
    
    /**
     * 曲目ID
     */
    @Schema(description = "曲目ID", example = "1001", required = true)
    private Long trackId;
    
    /**
     * 排名位置
     */
    @Schema(description = "排名位置", example = "1")
    private Integer rankPosition;
    
    /**
     * 排名分数
     * 综合考虑播放次数、点赞数等计算得出
     */
    @Schema(description = "排名分数", example = "95.5")
    private Double score;
    
    /**
     * 榜单生成时间
     */
    @Schema(description = "榜单生成时间")
    private LocalDateTime generatedAt;
}
