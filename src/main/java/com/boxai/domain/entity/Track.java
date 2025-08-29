package com.boxai.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 音乐曲目实体类（增强版 - 支持混合架构）
 * 记录可播放的音乐曲目信息，包括曲名、艺术家、分类、本地文件路径等
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_track")
@Schema(description = "音乐曲目信息")
public class Track extends BaseEntity {
    /**
     * 曲目主键ID
     */
    @TableId
    @Schema(description = "曲目ID", example = "1")
    private Long id;
    
    /**
     * 曲目名称
     */
    @Schema(description = "曲目名称", example = "月亮代表我的心", required = true)
    private String title;
    
    /**
     * 艺术家/歌手
     */
    @Schema(description = "艺术家/歌手", example = "邓丽君", required = true)
    private String artist;
    
    /**
     * 专辑名称
     */
    @Schema(description = "专辑名称", example = "邓丽君经典专辑")
    private String album;
    
    /**
     * 曲目分类
     * 如：流行、经典、摇滚、民谣等
     */
    @Schema(description = "曲目分类", example = "流行")
    private String category;
    
    /**
     * 语言
     * 如：中文、英文、日文、韩文等
     */
    @Schema(description = "语言", example = "中文")
    private String language;
    
    /**
     * 风格
     * 如：抒情、快歌、慢歌等
     */
    @Schema(description = "风格", example = "抒情")
    private String genre;
    
    /**
     * 曲目标签
     * 以逗号分隔的关键词，用于搜索和推荐
     */
    @Schema(description = "曲目标签(逗号分隔)", example = "经典,怀旧,温柔")
    private String tags;
    
    // ========== 本地媒体文件信息 ==========
    
    /**
     * 本地媒体文件路径
     * 存储在包间主机上的文件路径
     */
    @Schema(description = "本地媒体文件路径", example = "/media/tracks/001/月亮代表我的心.mp4")
    private String localFilePath;
    
    /**
     * 文件大小（字节）
     */
    @Schema(description = "文件大小(字节)", example = "52428800")
    private Long fileSize;
    
    /**
     * 时长（秒）
     */
    @Schema(description = "时长(秒)", example = "240")
    private Integer duration;
    
    /**
     * 视频质量
     * 如：HD、FHD、4K
     */
    @Schema(description = "视频质量", example = "FHD")
    private String videoQuality;
    
    /**
     * 音频质量
     * 如：128K、320K、FLAC
     */
    @Schema(description = "音频质量", example = "320K")
    private String audioQuality;
    
    // ========== 云端元数据 ==========
    
    /**
     * 封面图片URL
     */
    @Schema(description = "封面图片URL", example = "https://cdn.boxai.com/covers/track001.jpg")
    private String coverUrl;
    
    /**
     * 歌词文件URL
     */
    @Schema(description = "歌词文件URL", example = "https://cdn.boxai.com/lyrics/track001.lrc")
    private String lyricsUrl;
    
    /**
     * 试听片段URL
     */
    @Schema(description = "试听片段URL", example = "https://cdn.boxai.com/preview/track001.mp3")
    private String previewUrl;
    
    // ========== 推荐算法相关 ==========
    
    /**
     * 热度分数
     * 综合播放次数、点赞数等计算得出
     */
    @Schema(description = "热度分数", example = "85")
    private Integer hotScore;
    
    /**
     * 总播放次数
     */
    @Schema(description = "总播放次数", example = "12580")
    private Long playCount;
    
    /**
     * 点赞数
     */
    @Schema(description = "点赞数", example = "256")
    private Integer likeCount;
    
    /**
     * 近期播放次数（7天）
     */
    @Schema(description = "近期播放次数(7天)", example = "89")
    private Integer recentPlayCount;
    
    // ========== 状态标识 ==========
    
    /**
     * 是否新歌
     */
    @Schema(description = "是否新歌", example = "false")
    private Boolean isNew;
    
    /**
     * 是否热门
     */
    @Schema(description = "是否热门", example = "true")
    private Boolean isHot;
    
    /**
     * 是否推荐
     */
    @Schema(description = "是否推荐", example = "true")
    private Boolean isRecommended;
    
    /**
     * 状态
     * ACTIVE: 正常可用
     * INACTIVE: 暂时不可用
     * DELETED: 已删除
     */
    @Schema(description = "状态", example = "ACTIVE")
    private String status;
    
    // ========== 版权和同步信息 ==========
    
    /**
     * 版权信息
     */
    @Schema(description = "版权信息", example = "环球音乐集团")
    private String copyrightInfo;
    
    /**
     * 同步版本号
     * 用于本地文件同步控制
     */
    @Schema(description = "同步版本号", example = "1")
    private Long syncVersion;
    
    /**
     * 最后同步时间
     */
    @Schema(description = "最后同步时间")
    private LocalDateTime lastSyncAt;
}
