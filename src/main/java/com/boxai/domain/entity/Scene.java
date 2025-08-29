package com.boxai.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 场景实体类
 * 记录房间的视觉场景设置，支持多种主题场景
 */
@Data
@TableName("t_scene")
public class Scene extends BaseEntity {
    /**
     * 场景设置主键ID
     */
    @TableId
    private Long id;
    
    /**
     * 房间ID
     * 关联到t_room表的id字段
     */
    private Long roomId;
    
    /**
     * 场景类型
     * 如：梦幻、科技、自然、复古等主题场景
     */
    private String type;
    
    /**
     * 场景状态参数
     * JSON格式存储可视化效果和AI场景的配置参数
     */
    private String stateJson;
}


