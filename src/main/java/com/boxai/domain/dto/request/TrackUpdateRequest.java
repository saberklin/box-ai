package com.boxai.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 歌曲更新请求DTO
 */
@Data
@Schema(description = "歌曲更新请求")
public class TrackUpdateRequest {
    
    @NotNull(message = "歌曲ID不能为空")
    @Schema(description = "歌曲ID", example = "1001", required = true)
    private Long id;
    
    @Size(max = 128, message = "歌曲标题长度不能超过128字符")
    @Schema(description = "歌曲标题", example = "告白气球")
    private String title;
    
    @Size(max = 128, message = "歌手名长度不能超过128字符")
    @Schema(description = "歌手", example = "周杰伦")
    private String artist;
    
    @Size(max = 128, message = "专辑名长度不能超过128字符")
    @Schema(description = "专辑", example = "周杰伦的床边故事")
    private String album;
    
    @Size(max = 64, message = "分类长度不能超过64字符")
    @Schema(description = "歌曲分类", example = "流行")
    private String category;
    
    @Size(max = 32, message = "语言长度不能超过32字符")
    @Schema(description = "歌曲语言", example = "中文")
    private String language;
    
    @Size(max = 64, message = "风格长度不能超过64字符")
    @Schema(description = "音乐风格", example = "流行/R&B")
    private String genre;
    
    @Size(max = 256, message = "标签长度不能超过256字符")
    @Schema(description = "歌曲标签", example = "经典,热门,KTV必唱")
    private String tags;
    
    @Schema(description = "歌曲时长（秒）", example = "240")
    private Integer duration;
    
    @Schema(description = "视频质量", example = "1080P")
    private String videoQuality;
    
    @Schema(description = "音频质量", example = "320K")
    private String audioQuality;
    
    @Schema(description = "封面图片URL", example = "https://cdn.example.com/covers/12345.jpg")
    private String coverUrl;
    
    @Schema(description = "歌词文件URL", example = "https://cdn.example.com/lyrics/12345.lrc")
    private String lyricsUrl;
    
    @Schema(description = "预览片段URL", example = "https://cdn.example.com/preview/12345.mp3")
    private String previewUrl;
    
    @Schema(description = "版权信息", example = "华研国际音乐股份有限公司")
    private String copyrightInfo;
    
    @Schema(description = "是否为新歌", example = "true")
    private Boolean isNew;
    
    @Schema(description = "是否为热门", example = "false")
    private Boolean isHot;
    
    @Schema(description = "是否推荐", example = "false")
    private Boolean isRecommended;
    
    @Schema(description = "歌曲状态", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE", "DELETED"})
    private String status;
    
    @Schema(description = "强制更新同步版本", example = "true")
    private Boolean forceUpdateVersion = false;
}
