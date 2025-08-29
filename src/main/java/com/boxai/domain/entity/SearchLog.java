package com.boxai.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 搜索日志实体类
 * 记录用户的搜索行为，用于统计热门关键词和优化搜索算法
 */
@Data
@TableName("t_search_log")
public class SearchLog {
    /**
     * 搜索日志主键ID
     */
    @TableId
    private Long id;
    
    /**
     * 房间ID（可选）
     * 关联到t_room表的id字段，用于统计房间级别的热门搜索
     */
    private Long roomId;
    
    /**
     * 用户ID
     * 关联到t_user表的id字段
     */
    private Long userId;
    
    /**
     * 搜索关键词
     * 用户输入的搜索内容
     */
    private String keyword;
}


