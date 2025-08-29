package com.boxai.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 歌单推荐响应DTO
 * 用于返给前端的歌单推荐信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "歌单推荐信息")
public class PlaylistRecommendationDto {
    
    /**
     * 歌单ID
     */
    @Schema(description = "歌单ID", example = "1")
    private Long id;
    
    /**
     * 歌单名称
     */
    @Schema(description = "歌单名称", example = "本周热门")
    private String name;
    
    /**
     * 歌单描述
     */
    @Schema(description = "歌单描述", example = "本周最受欢迎的歌曲合集")
    private String description;
    
    /**
     * 推荐类型
     */
    @Schema(description = "推荐类型", example = "HOT")
    private String type;
    
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
     * 歌曲数量
     */
    @Schema(description = "歌曲数量", example = "20")
    private Integer trackCount;
    
    /**
     * 歌单中的部分歌曲（前3-5首用于预览）
     */
    @Schema(description = "歌单预览歌曲列表")
    private List<TrackRecommendationDto.TrackBasicInfo> previewTracks;
    
    /**
     * 排序权重
     */
    @Schema(description = "排序权重", example = "100")
    private Integer sortOrder;
}
