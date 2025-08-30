package com.boxai.service;

import com.boxai.domain.dto.device.AiVideoStreamCommand;
import com.boxai.domain.dto.device.DeviceControlCommand;
import com.boxai.domain.dto.device.LightingControlCommand;

/**
 * 设备控制服务
 * 负责将控制指令发布到消息通道（Redis Pub/Sub）供JavaFX桌面端消费
 */
public interface DeviceControlService {
    /**
     * 发布控制命令
     * @param command 控制命令
     */
    void publish(DeviceControlCommand command);

    /**
     * 发布灯光控制命令
     */
    void publishLighting(LightingControlCommand command);
    
    /**
     * 发布AI视频流控制命令
     * @param command AI视频流命令
     */
    void publishAiVideo(AiVideoStreamCommand command);
}


