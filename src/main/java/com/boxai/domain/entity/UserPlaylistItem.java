package com.boxai.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户播放列表项实体类
 * 记录用户播放列表中的具体曲目项，包括顺序信息
 */
@Data
@TableName("t_user_playlist_item")
public class UserPlaylistItem extends BaseEntity {
    /**
     * 播放列表项主键ID
     */
    @TableId
    private Long id;
    
    /**
     * 播放列表ID
     * 关联到t_user_playlist表的id字段
     */
    private Long playlistId;
    
    /**
     * 曲目ID
     * 关联到t_track表的id字段
     */
    private Long trackId;
    
    /**
     * 在播放列表中的位置
     * 数字越小越靠前
     */
    private Integer position;
}


