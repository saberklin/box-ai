package com.boxai.domain.dto.device;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "灯光控制命令")
public class LightingControlCommand {
    @Schema(description = "房间ID", example = "1", required = true)
    private Long roomId;

    @Schema(description = "亮度(0-100)", example = "80", required = true)
    private Integer brightness;

    @Schema(description = "颜色(HEX)", example = "#FF6B6B")
    private String color;

    @Schema(description = "节奏/模式", example = "SOFT")
    private String rhythm;

    @Schema(description = "时间戳(毫秒)")
    private long timestamp;
}


