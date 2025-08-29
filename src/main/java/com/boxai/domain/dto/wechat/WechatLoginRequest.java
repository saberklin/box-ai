package com.boxai.domain.dto.wechat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "微信登录请求")
public class WechatLoginRequest {
    @NotBlank
    @Schema(description = "微信小程序登录临时凭证", example = "081234567890abcdef", required = true)
    private String jsCode; // 小程序 wx.login() 返回的 code
    @Schema(description = "用户昵称", example = "音乐爱好者")
    private String nickname; // 可选：用户昵称
    @Schema(description = "用户头像URL", example = "https://example.com/avatar.jpg")
    private String avatarUrl; // 可选：头像
}


