package com.boxai.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 同步版本更新请求DTO
 */
@Data
@Schema(description = "同步版本更新请求")
public class SyncVersionUpdateRequest {
    
    @NotEmpty(message = "歌曲ID列表不能为空")
    @Schema(description = "歌曲ID列表", example = "[1001, 1002, 1003]", required = true)
    private List<Long> trackIds;
    
    @Schema(description = "更新原因", example = "批量更新高清版本")
    private String reason;
    
    @Schema(description = "是否强制推送到所有包间", example = "false")
    private Boolean forcePushToAllRooms = false;
    
    @Schema(description = "指定推送的包间ID列表", example = "[1, 2, 3]")
    private List<Long> targetRoomIds;
}
