package com.boxai.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 房间成员实体类
 * 记录用户与房间的关系，包括成员角色等信息
 */
@Data
@TableName("t_room_member")
public class RoomMember extends BaseEntity {
    /**
     * 成员关系主键ID
     */
    @TableId
    private Long id;
    
    /**
     * 房间ID
     * 关联到t_room表的id字段
     */
    private Long roomId;
    
    /**
     * 用户ID
     * 关联到t_user表的id字段
     */
    private Long userId;
    
    /**
     * 成员角色
     * OWNER：房主，NORMAL：普通成员
     */
    private String role;
}


