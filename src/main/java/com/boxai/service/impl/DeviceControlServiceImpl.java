package com.boxai.service.impl;

import com.boxai.domain.dto.device.AiVideoStreamCommand;
import com.boxai.domain.dto.device.DeviceControlCommand;
import com.boxai.domain.dto.device.LightingControlCommand;
import com.boxai.service.DeviceControlService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 设备控制服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceControlServiceImpl implements DeviceControlService {

    private static final String CHANNEL = "device:control";
    private static final String LIGHT_CHANNEL = "device:light";
    private static final String AI_VIDEO_CHANNEL = "device:ai-video";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(DeviceControlCommand command) {
        try {
            String payload = objectMapper.writeValueAsString(command);
            stringRedisTemplate.convertAndSend(CHANNEL, payload);
            log.info("已发布设备控制命令: {}", payload);
        } catch (JsonProcessingException e) {
            log.error("序列化控制命令失败", e);
            throw new RuntimeException("发布控制命令失败");
        }
    }

    @Override
    public void publishLighting(LightingControlCommand command) {
        try {
            String payload = objectMapper.writeValueAsString(command);
            stringRedisTemplate.convertAndSend(LIGHT_CHANNEL, payload);
            log.info("已发布灯光控制命令: {}", payload);
        } catch (JsonProcessingException e) {
            log.error("序列化灯光命令失败", e);
            throw new RuntimeException("发布灯光命令失败");
        }
    }
    
    @Override
    public void publishAiVideo(AiVideoStreamCommand command) {
        try {
            String payload = objectMapper.writeValueAsString(command);
            stringRedisTemplate.convertAndSend(AI_VIDEO_CHANNEL, payload);
            log.info("已发布AI视频流控制命令: streamId={}, action={}", command.getStreamId(), command.getAction());
        } catch (JsonProcessingException e) {
            log.error("序列化AI视频流命令失败", e);
            throw new RuntimeException("发布AI视频流命令失败");
        }
    }
}


