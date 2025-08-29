package com.boxai.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 播放历史实体类
 * 记录用户的曲目播放历史，包括播放时间、评分等信息
 */
@Data
@TableName("t_playback_history")
public class PlaybackHistory {
    /**
     * 播放历史主键ID
     */
    @TableId
    private Long id;
    
    /**
     * 房间ID
     * 关联到t_room表的id字段
     */
    private Long roomId;
    
    /**
     * 曲目ID
     * 关联到t_track表的id字段
     */
    private Long trackId;
    
    /**
     * 用户ID
     * 关联到t_user表的id字段
     */
    private Long userId;
    
    /**
     * 播放开始时间
     */
    private OffsetDateTime startedAt;
    
    /**
     * 播放结束时间
     */
    private OffsetDateTime endedAt;
    
    /**
     * 用户评分
     * 取值范围：1-5星，用于推荐算法
     */
    private Integer rating;
    
    /**
     * 是否为重播
     * true：重复播放，false：首次播放
     */
    private Boolean isReplay;
}


