package com.boxai.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 喜欢记录实体类
 * 记录用户对曲目的喜欢操作，用于个性化推荐
 */
@Data
@TableName("t_like")
public class LikeRecord extends BaseEntity {
    /**
     * 喜欢记录主键ID
     */
    @TableId
    private Long id;
    
    /**
     * 用户ID
     * 关联到t_user表的id字段
     */
    private Long userId;
    
    /**
     * 曲目ID
     * 关联到t_track表的id字段
     */
    private Long trackId;
}


