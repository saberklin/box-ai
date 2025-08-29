package com.boxai.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 灯光保存请求参数
 */
@Data
@Schema(description = "灯光保存请求")
public class LightingSaveRequest {
    /**
     * 房间ID
     */
    @Schema(description = "房间ID", example = "1", required = true)
    private Long roomId;
    
    /**
     * 亮度
     */
    @Min(0)
    @Max(100)
    @Schema(description = "亮度(0-100)", example = "80", required = true)
    private Integer brightness;
    
    /**
     * 颜色
     */
    @Schema(description = "颜色(十六进制)", example = "#FF6B6B", required = true)
    private String color;
    
    /**
     * 节奏模式
     */
    @Schema(description = "节奏模式", example = "SOFT", 
            allowableValues = {"SOFT", "NORMAL", "STRONG", "AUTO"})
    private String rhythm;
}
