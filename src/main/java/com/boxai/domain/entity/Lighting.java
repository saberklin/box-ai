package com.boxai.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 灯光实体类
 * 记录房间的灯光设置，包括亮度、颜色、韵律模式
 */
@Data
@TableName("t_lighting")
public class Lighting extends BaseEntity {
    /**
     * 灯光设置主键ID
     */
    @TableId
    private Long id;
    
    /**
     * 房间ID
     * 关联到t_room表的id字段
     */
    private Long roomId;
    
    /**
     * 灯光亮度
     * 取值范围：0-100，0为最暗，100为最亮
     */
    private Integer brightness;
    
    /**
     * 灯光颜色
     * HEX颜色码格式，如#FF0000（红色）
     */
    private String color;
    
    /**
     * 灯光韵律模式
     * 如：跟随音乐、慢闪、快闪、呼吸等模式
     */
    private String rhythm;
}


