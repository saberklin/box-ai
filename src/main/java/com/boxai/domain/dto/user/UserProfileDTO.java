package com.boxai.domain.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户个人资料")
public class UserProfileDTO {
    @Schema(description = "用户ID", example = "123")
    private Long id;
    @Schema(description = "微信用户唯一标识", example = "ox1234567890abcdef")
    private String openId;
    @Schema(description = "用户昵称", example = "音乐爱好者")
    private String nickname;
    @Schema(description = "用户头像URL", example = "https://example.com/avatar.jpg")
    private String avatarUrl;
    @Schema(description = "用户手机号", example = "13800138000")
    private String phone;
}


