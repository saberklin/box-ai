package com.boxai.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户实体类
 * 记录用户的基本信息，包括微信身份标识、昵称、头像等
 */
@Data
@TableName("t_user")
@Schema(description = "用户信息")
public class User extends BaseEntity {
    /**
     * 用户主键ID
     */
    @TableId
    @Schema(description = "用户ID", example = "1")
    private Long id;
    
    /**
     * 微信用户唯一标识
     */
    @Schema(description = "微信用户唯一标识", example = "ox1234567890abcdef")
    private String openId;
    
    /**
     * 用户昵称
     */
    @Schema(description = "用户昵称", example = "音乐爱好者")
    private String nickname;
    
    /**
     * 用户头像URL
     */
    @Schema(description = "用户头像URL", example = "https://example.com/avatar.jpg")
    private String avatarUrl;
    
    /**
     * 用户手机号（可选）
     */
    @Schema(description = "用户手机号", example = "13800138000")
    private String phone;
}


