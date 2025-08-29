package com.boxai.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 房间实体类
 * 记录KTV房间的基本信息，包括房间编号、名称、房主等
 */
@Data
@TableName("t_room")
@Schema(description = "KTV房间信息")
public class Room extends BaseEntity {
    /**
     * 房间主键ID
     */
    @TableId
    @Schema(description = "房间ID", example = "1")
    private Long id;
    
    /**
     * 房间编号/二维码标识
     * 用于用户扫码进入房间
     */
    @Schema(description = "房间编号", example = "R001", required = true)
    private String roomCode;
    
    /**
     * 房间名称
     */
    @Schema(description = "房间名称", example = "豪华包厢A")
    private String name;
    
    /**
     * 房主用户ID
     * 关联到t_user表的id字段
     */
    @Schema(description = "房主ID", example = "123")
    private Long ownerUserId;
    
    /**
     * 二维码版本号
     * 用于控制二维码的有效性，每次重置房间时递增
     */
    @Schema(description = "二维码版本号", example = "1")
    private Integer qrVersion;
    
    /**
     * 房间状态
     * ACTIVE：活跃中，RESET：已重置，CLOSED：已关闭
     */
    @Schema(description = "房间状态", example = "ACTIVE", allowableValues = {"ACTIVE", "RESET", "CLOSED"})
    private String status;
}


