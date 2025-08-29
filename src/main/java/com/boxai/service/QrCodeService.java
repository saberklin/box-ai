package com.boxai.service;

import com.boxai.domain.dto.wechat.RoomQrResponse;

/**
 * 二维码生成服务接口
 * 专门负责微信小程序二维码的生成和管理
 */
public interface QrCodeService {
    /**
     * 生成房间专属小程序码
     * 为指定房间生成带有房间信息的微信小程序二维码，用于快速进入房间
     * @param roomId 房间ID
     * @param page 小程序页面路径（可选，默认为首页）
     * @return 二维码响应（包含房间ID和Base64编码的二维码图片）
     */
    RoomQrResponse generateRoomQr(Long roomId, String page);
    
    /**
     * 为指定房间和版本生成二维码
     * @param roomId 房间ID
     * @param version 二维码版本号
     * @param page 小程序页面路径（可选）
     * @return 二维码响应
     */
    RoomQrResponse generateRoomQrWithVersion(Long roomId, Integer version, String page);
}
