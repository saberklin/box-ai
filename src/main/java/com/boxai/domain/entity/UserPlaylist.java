package com.boxai.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户播放列表实体类
 * 记录用户创建的个人播放列表，用于收藏喜欢的曲目
 */
@Data
@TableName("t_user_playlist")
public class UserPlaylist extends BaseEntity {
    /**
     * 播放列表主键ID
     */
    @TableId
    private Long id;
    
    /**
     * 用户ID
     * 关联到t_user表的id字段
     */
    private Long userId;
    
    /**
     * 播放列表名称
     * 用户自定义的播放列表名称
     */
    private String name;
}


