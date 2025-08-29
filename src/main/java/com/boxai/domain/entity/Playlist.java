package com.boxai.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 播放列表实体类
 * 记录房间的播放队列，包括曲目顺序、播放状态等
 */
@Data
@TableName("t_playlist")
public class Playlist extends BaseEntity {
    /**
     * 播放列表项主键ID
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
     * 点歌用户ID
     * 关联到t_user表的id字段
     */
    private Long orderedByUserId;
    
    /**
     * 在播放队列中的位置
     * 数字越小越靠前
     */
    private Integer position;
    
    /**
     * 播放状态
     * QUEUED：已排队，PLAYING：正在播放，DONE：播放完成
     */
    private String status;
}


