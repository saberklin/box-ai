package com.boxai.domain.dto.device;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 设备控制命令
 * JavaFX 桌面端订阅 Redis 渠道后接收该命令并执行对应操作
 */
@Data
@Schema(description = "设备控制命令")
public class DeviceControlCommand {
    @Schema(description = "房间ID", example = "1", required = true)
    private Long roomId;

    @Schema(description = "控制动作", example = "PLAY", required = true)
    private String action;

    @Schema(description = "曲目ID（部分动作可选）", example = "123")
    private Long trackId;

    @Schema(description = "发起用户ID", example = "456")
    private Long userId;

    @Schema(description = "时间戳(毫秒)", example = "1718000000000")
    private long timestamp;
}


