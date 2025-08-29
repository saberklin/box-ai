package com.boxai.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 场景保存请求参数
 */
@Data
@Schema(description = "场景保存请求")
public class SceneSaveRequest {
    /**
     * 房间ID
     */
    @Schema(description = "房间ID", example = "1", required = true)
    private Long roomId;
    
    /**
     * 场景类型
     */
    @Schema(description = "场景类型", example = "梦幻", 
            allowableValues = {"梦幻", "科技", "自然", "复古", "现代"}, 
            required = true)
    private String type;
    
    /**
     * 场景状态JSON
     */
    @Schema(description = "场景状态JSON配置", 
            example = "{\"brightness\": 80, \"color\": \"#FF6B6B\", \"effect\": \"fade\"}")
    private String stateJson;
}
