package com.boxai.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.boxai.common.web.ApiResponse;
import com.boxai.domain.entity.Lighting;
import com.boxai.service.LightingService;
import com.boxai.service.DeviceControlService;
import com.boxai.domain.dto.device.LightingControlCommand;
import com.boxai.domain.dto.request.LightingSaveRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

/**
 * 灯光控制器
 * 提供房间灯光设置和管理功能
 */
@RestController
@RequestMapping("/api/lighting")
@RequiredArgsConstructor
@Tag(name = "灯光控制", description = "KTV房间灯光设置相关API")
public class LightingController {
    private final LightingService lightingService;
    private final DeviceControlService deviceControlService;

    /**
     * 保存房间灯光设置
     * 创建或更新指定房间的灯光配置（亮度、颜色、韵律）
     * @param req 灯光保存请求（包含房间ID、亮度、颜色、韵律）
     * @return 保存后的灯光信息
     */
    @PostMapping
    @Operation(summary = "保存灯光设置", 
               description = "保存房间的灯光亮度、颜色和节奏设置")

    public ApiResponse<Lighting> save(
            @Parameter(description = "灯光保存请求参数", required = true)
            @RequestBody LightingSaveRequest req) {
        Lighting l = lightingService.getOne(Wrappers.<Lighting>lambdaQuery().eq(Lighting::getRoomId, req.getRoomId()));
        if (l == null) {
            l = new Lighting();
            l.setRoomId(req.getRoomId());
            l.setBrightness(req.getBrightness());
            l.setColor(req.getColor());
            l.setRhythm(req.getRhythm());
            lightingService.save(l);
        } else {
            l.setBrightness(req.getBrightness());
            l.setColor(req.getColor());
            l.setRhythm(req.getRhythm());
            lightingService.updateById(l);
        }
        // 发布灯光控制命令到桌面端
        LightingControlCommand cmd = new LightingControlCommand();
        cmd.setRoomId(l.getRoomId());
        cmd.setBrightness(l.getBrightness());
        cmd.setColor(l.getColor());
        cmd.setRhythm(l.getRhythm());
        cmd.setTimestamp(System.currentTimeMillis());
        deviceControlService.publishLighting(cmd);
        return ApiResponse.ok(l);
    }

    /**
     * 获取房间灯光设置
     * @param roomId 房间ID
     * @return 房间当前的灯光设置
     */
    @GetMapping("/{roomId}")
    public ApiResponse<Lighting> get(@PathVariable Long roomId) {
        return ApiResponse.ok(lightingService.getOne(Wrappers.<Lighting>lambdaQuery().eq(Lighting::getRoomId, roomId)));
    }


}


