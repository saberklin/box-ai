package com.boxai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.boxai.domain.dto.AiVideoStreamResponse;
import com.boxai.domain.dto.device.AiVideoStreamCommand;
import com.boxai.domain.dto.request.AiVideoGenerateRequest;
import com.boxai.domain.entity.AiVideoSession;
import com.boxai.domain.mapper.AiVideoSessionMapper;
import com.boxai.service.AiVideoGenerationClient;
import com.boxai.service.AiVideoService;
import com.boxai.service.DeviceControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * AI视频生成服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiVideoServiceImpl implements AiVideoService {
    
    private final AiVideoSessionMapper aiVideoSessionMapper;
    private final DeviceControlService deviceControlService;
    private final AiVideoGenerationClient aiVideoGenerationClient;
    
    // AI视频生成服务的基础URL（实际部署时需要配置）
    private static final String AI_VIDEO_SERVICE_URL = "http://localhost:8888";
    private static final String STREAM_SERVER_URL = "rtmp://localhost:1935/live";
    private static final String HLS_SERVER_URL = "http://localhost:8080/hls";
    private static final String WEBRTC_SERVER_URL = "webrtc://localhost:8080/stream";
    
    @Override
    @Transactional
    public AiVideoStreamResponse startVideoGeneration(AiVideoGenerateRequest request) {
        // 生成唯一的流ID
        String streamId = "ai_stream_" + UUID.randomUUID().toString().replace("-", "");
        
        // 创建视频生成会话
        AiVideoSession session = new AiVideoSession();
        BeanUtils.copyProperties(request, session);
        session.setStreamId(streamId);
        session.setStatus("PENDING");
        session.setProgress(0);
        session.setStartTime(LocalDateTime.now());
        
        // 生成流媒体URL
        session.setStreamUrl(STREAM_SERVER_URL + "/" + streamId);
        session.setHlsUrl(HLS_SERVER_URL + "/" + streamId + ".m3u8");
        session.setWebrtcUrl(WEBRTC_SERVER_URL + "/" + streamId);
        session.setBitrate(calculateBitrate(request.getResolution(), request.getFrameRate()));
        
        aiVideoSessionMapper.insert(session);
        
        // 异步启动AI视频生成
        CompletableFuture.runAsync(() -> {
            try {
                startAiVideoGenerationWithClient(session, request);
            } catch (Exception e) {
                log.error("AI视频生成失败: streamId={}", streamId, e);
                updateSessionStatus(streamId, "FAILED", 0, e.getMessage());
            }
        });
        
        // 返回响应
        AiVideoStreamResponse response = new AiVideoStreamResponse();
        BeanUtils.copyProperties(session, response);
        response.setEstimatedTimeRemaining(request.getDuration());
        response.setEstimatedEndTime(LocalDateTime.now().plusSeconds(request.getDuration()));
        
        log.info("开始AI视频生成: streamId={}, roomId={}, type={}", streamId, request.getRoomId(), request.getVideoType());
        
        return response;
    }
    
    @Override
    public AiVideoStreamResponse getGenerationStatus(String streamId) {
        AiVideoSession session = getSessionByStreamId(streamId);
        if (session == null) {
            throw new RuntimeException("视频流不存在: " + streamId);
        }
        
        AiVideoStreamResponse response = new AiVideoStreamResponse();
        BeanUtils.copyProperties(session, response);
        
        // 计算预计剩余时间
        if ("GENERATING".equals(session.getStatus()) && session.getProgress() > 0) {
            int remainingProgress = 100 - session.getProgress();
            long elapsedTime = java.time.Duration.between(session.getStartTime(), LocalDateTime.now()).getSeconds();
            int estimatedRemaining = (int) (elapsedTime * remainingProgress / session.getProgress());
            response.setEstimatedTimeRemaining(estimatedRemaining);
        }
        
        return response;
    }
    
    @Override
    @Transactional
    public void stopVideoGeneration(String streamId) {
        AiVideoSession session = getSessionByStreamId(streamId);
        if (session == null) {
            throw new RuntimeException("视频流不存在: " + streamId);
        }
        
        // 停止AI生成进程
        try {
            stopAiVideoGeneration(streamId);
        } catch (Exception e) {
            log.error("停止AI视频生成失败: streamId={}", streamId, e);
        }
        
        // 更新会话状态
        session.setStatus("STOPPED");
        session.setEndTime(LocalDateTime.now());
        aiVideoSessionMapper.updateById(session);
        
        // 通知桌面端停止播放
        AiVideoStreamCommand command = new AiVideoStreamCommand();
        command.setRoomId(session.getRoomId());
        command.setStreamId(streamId);
        command.setAction("STOP_STREAM");
        command.setTimestamp(System.currentTimeMillis());
        
        deviceControlService.publishAiVideo(command);
        
        log.info("停止AI视频生成: streamId={}", streamId);
    }
    
    @Override
    public List<AiVideoStreamResponse> getActiveStreams(Long roomId) {
        QueryWrapper<AiVideoSession> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("room_id", roomId)
                   .in("status", "GENERATING", "STREAMING")
                   .orderByDesc("start_time");
        
        List<AiVideoSession> sessions = aiVideoSessionMapper.selectList(queryWrapper);
        
        return sessions.stream()
                .map(session -> {
                    AiVideoStreamResponse response = new AiVideoStreamResponse();
                    BeanUtils.copyProperties(session, response);
                    return response;
                })
                .collect(Collectors.toList());
    }
    
    @Override
    public void pushStreamToDesktop(String streamId, Long roomId) {
        AiVideoSession session = getSessionByStreamId(streamId);
        if (session == null) {
            throw new RuntimeException("视频流不存在: " + streamId);
        }
        
        // 发送流推送命令到桌面端
        AiVideoStreamCommand command = new AiVideoStreamCommand();
        command.setRoomId(roomId);
        command.setStreamId(streamId);
        command.setAction("START_STREAM");
        command.setStreamUrl(session.getStreamUrl());
        command.setHlsUrl(session.getHlsUrl());
        command.setWebrtcUrl(session.getWebrtcUrl());
        command.setResolution(session.getResolution());
        command.setFrameRate(session.getFrameRate());
        command.setTimestamp(System.currentTimeMillis());
        
        deviceControlService.publishAiVideo(command);
        
        log.info("推送AI视频流到桌面端: streamId={}, roomId={}", streamId, roomId);
    }
    
    @Override
    @Transactional
    public void updateGenerationProgress(String streamId, Integer progress, String status) {
        AiVideoSession session = getSessionByStreamId(streamId);
        if (session == null) {
            log.warn("更新进度时视频流不存在: streamId={}", streamId);
            return;
        }
        
        session.setProgress(progress);
        session.setStatus(status);
        
        if ("STREAMING".equals(status)) {
            // 视频开始流式传输，推送到桌面端
            pushStreamToDesktop(streamId, session.getRoomId());
        } else if ("COMPLETED".equals(status)) {
            session.setEndTime(LocalDateTime.now());
        }
        
        aiVideoSessionMapper.updateById(session);
        
        log.debug("更新AI视频生成进度: streamId={}, progress={}%, status={}", streamId, progress, status);
    }
    
    @Override
    public List<AiVideoSession> getGenerationHistory(Long roomId, Integer limit) {
        QueryWrapper<AiVideoSession> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("room_id", roomId)
                   .orderByDesc("start_time")
                   .last("LIMIT " + (limit != null ? limit : 50));
        
        return aiVideoSessionMapper.selectList(queryWrapper);
    }
    
    /**
     * 使用AI客户端启动视频生成
     */
    private void startAiVideoGenerationWithClient(AiVideoSession session, AiVideoGenerateRequest request) {
        String streamId = session.getStreamId();
        
        try {
            log.info("开始AI视频生成: streamId={}, provider={}", streamId, aiVideoGenerationClient.getClass().getSimpleName());
            
            // 1. 启动AI生成任务
            String taskId = aiVideoGenerationClient.startGeneration(request, session)
                    .block(Duration.ofSeconds(30));
            
            if (taskId == null) {
                throw new RuntimeException("启动AI生成任务失败");
            }
            
            // 更新会话信息
            session.setGenerationInfo("AI Task ID: " + taskId);
            aiVideoSessionMapper.updateById(session);
            
            log.info("AI生成任务启动成功: streamId={}, taskId={}", streamId, taskId);
            
            // 2. 监听生成进度
            aiVideoGenerationClient.getProgressStream(taskId)
                    .doOnNext(progress -> {
                        log.debug("收到进度更新: streamId={}, progress={}%, status={}", 
                                streamId, progress.progress(), progress.status());
                        
                        // 更新数据库状态
                        updateSessionStatus(streamId, progress.status(), progress.progress(), progress.message());
                        
                        // 当状态变为STREAMING时，推送到桌面端
                        if ("STREAMING".equals(progress.status()) || 
                            ("COMPLETED".equals(progress.status()) && progress.progress() >= 100)) {
                            
                            // 获取生成结果并推送流
                            aiVideoGenerationClient.getGenerationResult(taskId)
                                    .subscribe(result -> {
                                        if (result.streamUrl() != null) {
                                            // 更新会话的流URL
                                            AiVideoSession updatedSession = getSessionByStreamId(streamId);
                                            if (updatedSession != null) {
                                                updatedSession.setStreamUrl(result.streamUrl());
                                                updatedSession.setHlsUrl(result.hlsUrl());
                                                updatedSession.setWebrtcUrl(result.webrtcUrl());
                                                aiVideoSessionMapper.updateById(updatedSession);
                                            }
                                            
                                            // 推送到桌面端
                                            pushStreamToDesktop(streamId, session.getRoomId());
                                        }
                                    }, error -> {
                                        log.error("获取生成结果失败: streamId={}", streamId, error);
                                    });
                        }
                    })
                    .doOnComplete(() -> {
                        log.info("AI视频生成进度流完成: streamId={}", streamId);
                    })
                    .doOnError(error -> {
                        log.error("AI视频生成进度流出错: streamId={}", streamId, error);
                        updateSessionStatus(streamId, "FAILED", 0, error.getMessage());
                    })
                    .subscribe();
                    
        } catch (Exception e) {
            log.error("AI视频生成启动失败: streamId={}", streamId, e);
            updateSessionStatus(streamId, "FAILED", 0, e.getMessage());
        }
    }
    
    @Override
    public AiVideoSession getSessionByStreamId(String streamId) {
        QueryWrapper<AiVideoSession> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("stream_id", streamId);
        return aiVideoSessionMapper.selectOne(queryWrapper);
    }
    
    @Override
    public List<AiVideoSession> getActiveSessions(Integer limit) {
        QueryWrapper<AiVideoSession> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("status", "PENDING", "GENERATING", "STREAMING")
                   .orderByDesc("created_at")
                   .last("LIMIT " + (limit != null ? limit : 50));
        
        return aiVideoSessionMapper.selectList(queryWrapper);
    }
    

    
    /**
     * 停止AI视频生成
     */
    private void stopAiVideoGeneration(String streamId) {
        log.info("停止AI视频生成: streamId={}", streamId);
        
        try {
            AiVideoSession session = getSessionByStreamId(streamId);
            if (session == null) {
                log.warn("会话不存在，无法停止: streamId={}", streamId);
                return;
            }
            
            // 从生成信息中提取任务ID
            String generationInfo = session.getGenerationInfo();
            if (generationInfo != null && generationInfo.contains("AI Task ID: ")) {
                String taskId = generationInfo.substring(generationInfo.indexOf("AI Task ID: ") + 12);
                
                // 调用AI客户端停止任务
                aiVideoGenerationClient.stopGeneration(taskId)
                        .subscribe(success -> {
                            if (success) {
                                log.info("AI生成任务停止成功: streamId={}, taskId={}", streamId, taskId);
                            } else {
                                log.warn("AI生成任务停止失败: streamId={}, taskId={}", streamId, taskId);
                            }
                        }, error -> {
                            log.error("停止AI生成任务出错: streamId={}, taskId={}", streamId, taskId, error);
                        });
            }
            
            // 更新会话状态
            updateSessionStatus(streamId, "STOPPED", null, "用户停止");
            
        } catch (Exception e) {
            log.error("停止AI视频生成失败: streamId={}", streamId, e);
            updateSessionStatus(streamId, "FAILED", null, "停止失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新会话状态
     */
    private void updateSessionStatus(String streamId, String status, Integer progress, String errorMessage) {
        AiVideoSession session = getSessionByStreamId(streamId);
        if (session != null) {
            session.setStatus(status);
            session.setProgress(progress);
            if (errorMessage != null) {
                session.setErrorMessage(errorMessage);
            }
            if ("COMPLETED".equals(status) || "FAILED".equals(status) || "STOPPED".equals(status)) {
                session.setEndTime(LocalDateTime.now());
            }
            aiVideoSessionMapper.updateById(session);
        }
    }
    
    /**
     * 计算比特率
     */
    private Integer calculateBitrate(String resolution, Integer frameRate) {
        // 根据分辨率和帧率计算合适的比特率
        if ("1920x1080".equals(resolution)) {
            return frameRate * 200; // 1080p: ~6000kbps for 30fps
        } else if ("1280x720".equals(resolution)) {
            return frameRate * 120; // 720p: ~3600kbps for 30fps
        } else {
            return frameRate * 80; // 默认: ~2400kbps for 30fps
        }
    }
}
