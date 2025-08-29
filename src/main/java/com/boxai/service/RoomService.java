package com.boxai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.boxai.domain.entity.Room;
import com.boxai.domain.dto.room.RoomResetResponse;

/**
 * 房间服务接口
 * 提供房间信息的CRUD操作和业务逻辑
 */
public interface RoomService extends IService<Room> {
    /**
     * 重置房间
     * 清空成员、播放列表，更新二维码版本，返回新的二维码
     * @param roomId 房间ID
     * @return 重置结果（包含房间信息和新二维码）
     */
    RoomResetResponse resetRoom(Long roomId);
    
    /**
     * 刷新房间二维码版本
     * 更新房间的二维码版本号，使旧的二维码失效
     * @param roomId 房间ID
     * @return 新的版本号
     */
    Integer refreshQrVersion(Long roomId);
}


