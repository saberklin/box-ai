package com.boxai.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.boxai.common.web.ApiResponse;
import com.boxai.domain.entity.Playlist;
import com.boxai.domain.dto.request.PlaybackControlRequest;
import com.boxai.domain.dto.request.PlaybackQueueRequest;
import com.boxai.service.PlaylistService;
import com.boxai.service.DeviceControlService;
import com.boxai.domain.dto.device.DeviceControlCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * 播放控制器
 * 提供曲目队列管理和播放控制功能
 */
@RestController
@RequestMapping("/api/playback")
@Validated
@RequiredArgsConstructor
@Tag(name = "播放控制", description = "音乐播放和队列管理相关API")
public class PlaybackController {
    private final PlaylistService playlistService;
    private final DeviceControlService deviceControlService;

    /**
     * 添加曲目到播放队列
     * 将指定曲目添加到房间的播放队列末尾
     * @param req 排队请求（包含房间ID、曲目ID、用户ID）
     * @return 操作结果
     */
    @PostMapping("/queue")
    @Operation(summary = "添加到播放队列",
               description = "将指定曲目添加到房间的播放队列中")
    public ApiResponse<String> queue(
            @Parameter(description = "播放队列请求参数", required = true)
            @RequestBody PlaybackQueueRequest req) {
        Integer maxPos = playlistService.list(Wrappers.<Playlist>lambdaQuery().eq(Playlist::getRoomId, req.getRoomId()))
                .stream().map(Playlist::getPosition).max(Integer::compareTo).orElse(0);
        Playlist p = new Playlist();
        p.setRoomId(req.getRoomId());
        p.setTrackId(req.getTrackId());
        p.setOrderedByUserId(req.getUserId());
        p.setPosition(maxPos + 1);
        p.setStatus("QUEUED");
        playlistService.save(p);
        return ApiResponse.ok("queued");
    }

    /**
     * 获取房间播放队列
     * 按位置顺序返回指定房间的播放队列
     * @param roomId 房间ID
     * @return 播放队列列表
     */
    @GetMapping("/queue/{roomId}")
    public ApiResponse<List<Playlist>> list(@PathVariable Long roomId) {
        List<Playlist> list = playlistService.list(new LambdaQueryWrapper<Playlist>()
                .eq(Playlist::getRoomId, roomId)
                .orderByAsc(Playlist::getPosition));
        return ApiResponse.ok(list);
    }

    /**
     * 播放控制
     * 控制房间的播放状态（播放、暂停、下一首等）
     * @param req 控制请求（包含房间ID和操作类型）
     * @return 操作结果
     */
    @PostMapping("/control")
    @Operation(summary = "播放控制",
               description = "控制房间的播放状态，并将命令发布给设备端(JavaFX)执行")
    public ApiResponse<String> control(
            @Parameter(description = "播放控制请求参数", required = true)
            @RequestBody PlaybackControlRequest req) {
        DeviceControlCommand cmd = new DeviceControlCommand();
        cmd.setRoomId(req.getRoomId());
        cmd.setAction(req.getAction());
        cmd.setTimestamp(System.currentTimeMillis());
        deviceControlService.publish(cmd);
        return ApiResponse.ok("published:" + req.getAction());
    }

    

    
}


