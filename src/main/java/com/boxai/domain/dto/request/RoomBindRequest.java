package com.boxai.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 房间绑定请求参数
 */
@Data
@Schema(description = "房间绑定请求")
public class RoomBindRequest {
    /**
     * 房间编号
     */
    @NotBlank
    @Schema(description = "房间编号", example = "R001", required = true)
    private String roomCode;
    
    /**
     * 用户ID
     */
    @Schema(description = "用户ID", example = "123")
    private Long userId;
}
