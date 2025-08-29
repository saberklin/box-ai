package com.boxai.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.boxai.common.web.ApiResponse;
import com.boxai.domain.entity.Scene;
import com.boxai.service.SceneService;
import com.boxai.domain.dto.request.SceneSaveRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

/**
 * 场景控制器
 * 提供房间场景设置和管理功能
 */
@RestController
@RequestMapping("/api/scenes")
@RequiredArgsConstructor
@Tag(name = "场景管理", description = "KTV房间场景设置相关API")
public class SceneController {
    private final SceneService sceneService;

    /**
     * 保存房间场景设置
     * 创建或更新指定房间的场景配置
     * @param req 场景保存请求（包含房间ID、类型、状态JSON）
     * @return 保存后的场景信息
     */
    @PostMapping
    @Operation(summary = "保存场景设置", 
               description = "保存房间的场景设置信息")

    public ApiResponse<Scene> save(
            @Parameter(description = "场景保存请求参数", required = true)
            @RequestBody SceneSaveRequest req) {
        Scene s = sceneService.getOne(Wrappers.<Scene>lambdaQuery().eq(Scene::getRoomId, req.getRoomId()));
        if (s == null) {
            s = new Scene();
            s.setRoomId(req.getRoomId());
            s.setType(req.getType());
            s.setStateJson(req.getStateJson());
            sceneService.save(s);
        } else {
            s.setType(req.getType());
            s.setStateJson(req.getStateJson());
            sceneService.updateById(s);
        }
        return ApiResponse.ok(s);
    }

    /**
     * 获取房间场景设置
     * @param roomId 房间ID
     * @return 房间当前的场景设置
     */
    @GetMapping("/{roomId}")
    public ApiResponse<Scene> get(@PathVariable Long roomId) {
        Scene s = sceneService.getOne(Wrappers.<Scene>lambdaQuery().eq(Scene::getRoomId, roomId));
        return ApiResponse.ok(s);
    }


}


