package com.boxai.domain.dto.room;

import com.boxai.domain.entity.Room;
import com.boxai.domain.dto.wechat.RoomQrResponse;
import lombok.Data;

/**
 * 房间重置响应DTO
 * 包含重置后的房间信息和新的二维码
 */
@Data
public class RoomResetResponse {
    /**
     * 重置后的房间信息
     */
    private Room room;
    
    /**
     * 新生成的二维码
     */
    private RoomQrResponse newQr;
    
    /**
     * 操作结果消息
     */
    private String message;
}
