package com.boxai.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 歌曲推荐响应DTO
 * 用于返给前端的歌曲推荐信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "歌曲推荐信息")
public class TrackRecommendationDto {
    
    /**
     * 曲目基本信息
     */
    @Schema(description = "曲目基本信息")
    private TrackBasicInfo track;
    
    /**
     * 推荐原因
     */
    @Schema(description = "推荐原因", example = "基于您的听歌偏好")
    private String recommendReason;
    
    /**
     * 推荐分数
     */
    @Schema(description = "推荐分数", example = "0.95")
    private Double recommendScore;
    
    /**
     * 是否热门
     */
    @Schema(description = "是否热门", example = "true")
    private Boolean isHot;
    
    /**
     * 是否新歌
     */
    @Schema(description = "是否新歌", example = "false")
    private Boolean isNew;
    
    /**
     * 相似歌曲推荐
     */
    @Schema(description = "相似歌曲ID列表")
    private List<Long> similarTrackIds;
    
    /**
     * 曲目基本信息内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "曲目基本信息")
    public static class TrackBasicInfo {
        @Schema(description = "曲目ID", example = "1001")
        private Long id;
        
        @Schema(description = "歌曲名称", example = "月亮代表我的心")
        private String title;
        
        @Schema(description = "歌手", example = "郓丽君")
        private String artist;
        
        @Schema(description = "专辑", example = "郓丽君经典专辑")
        private String album;
        
        @Schema(description = "分类", example = "流行")
        private String category;
        
        @Schema(description = "语言", example = "中文")
        private String language;
        
        @Schema(description = "风格", example = "抗情")
        private String genre;
        
        @Schema(description = "封面图片URL", example = "https://cdn.boxai.com/covers/track001.jpg")
        private String coverUrl;
        
        @Schema(description = "时长(秒)", example = "240")
        private Integer duration;
        
        @Schema(description = "热度分数", example = "85")
        private Integer hotScore;
        
        @Schema(description = "播放次数", example = "12580")
        private Long playCount;
        
        @Schema(description = "点赞数", example = "256")
        private Integer likeCount;
    }
}
