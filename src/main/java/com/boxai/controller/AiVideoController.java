package com.boxai.controller;

import com.boxai.common.web.ApiResponse;
import com.boxai.domain.dto.AiVideoStreamResponse;
import com.boxai.domain.dto.request.AiVideoGenerateRequest;
import com.boxai.domain.entity.AiVideoSession;
import com.boxai.service.AiVideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * AI视频生成控制器
 * 负责处理AI实时视频生成、流推送和大屏输出控制
 */
@RestController
@RequestMapping("/api/ai-video")
@RequiredArgsConstructor
@Tag(name = "AI视频生成", description = "AI实时视频生成、流推送和大屏输出控制功能")
public class AiVideoController {
    
    private final AiVideoService aiVideoService;
    
    @PostMapping("/generate")
    @Operation(summary = "开始AI视频生成", description = "根据用户输入的提示词和参数生成AI视频并推送到桌面端大屏显示")
    public ApiResponse<AiVideoStreamResponse> generateVideo(
            @Parameter(description = "AI视频生成请求", required = true) @Valid @RequestBody AiVideoGenerateRequest request
    ) {
        AiVideoStreamResponse response = aiVideoService.startVideoGeneration(request);
        return ApiResponse.success(response);
    }
    
    @GetMapping("/status/{streamId}")
    @Operation(summary = "获取视频生成状态", description = "查询AI视频生成的实时状态和进度")
    public ApiResponse<AiVideoStreamResponse> getGenerationStatus(
            @Parameter(description = "视频流ID", required = true, example = "ai_stream_12345") @PathVariable String streamId
    ) {
        AiVideoStreamResponse response = aiVideoService.getGenerationStatus(streamId);
        return ApiResponse.success(response);
    }
    
    @PostMapping("/stop/{streamId}")
    @Operation(summary = "停止视频生成", description = "停止指定的AI视频生成任务和流推送")
    public ApiResponse<Void> stopGeneration(
            @Parameter(description = "视频流ID", required = true, example = "ai_stream_12345") @PathVariable String streamId
    ) {
        aiVideoService.stopVideoGeneration(streamId);
        return ApiResponse.success();
    }
    
    @GetMapping("/active/{roomId}")
    @Operation(summary = "获取包间活跃视频流", description = "获取指定包间当前正在生成或播放的AI视频流列表")
    public ApiResponse<List<AiVideoStreamResponse>> getActiveStreams(
            @Parameter(description = "包间ID", required = true, example = "1001") @PathVariable Long roomId
    ) {
        List<AiVideoStreamResponse> streams = aiVideoService.getActiveStreams(roomId);
        return ApiResponse.success(streams);
    }
    
    @PostMapping("/push/{streamId}")
    @Operation(summary = "推送视频流到桌面端", description = "将AI生成的视频流推送到指定包间的桌面端大屏显示")
    public ApiResponse<Void> pushStreamToDesktop(
            @Parameter(description = "视频流ID", required = true, example = "ai_stream_12345") @PathVariable String streamId,
            @Parameter(description = "包间ID", required = true, example = "1001") @RequestParam Long roomId
    ) {
        aiVideoService.pushStreamToDesktop(streamId, roomId);
        return ApiResponse.success();
    }
    
    @GetMapping("/history/{roomId}")
    @Operation(summary = "获取视频生成历史", description = "获取指定包间的AI视频生成历史记录")
    public ApiResponse<List<AiVideoSession>> getGenerationHistory(
            @Parameter(description = "包间ID", required = true, example = "1001") @PathVariable Long roomId,
            @Parameter(description = "记录数量限制", example = "20") @RequestParam(defaultValue = "20") Integer limit
    ) {
        List<AiVideoSession> history = aiVideoService.getGenerationHistory(roomId, limit);
        return ApiResponse.success(history);
    }
    
    @PostMapping("/progress/{streamId}")
    @Operation(summary = "更新生成进度", description = "内部接口：更新AI视频生成进度（由AI生成服务调用）")
    public ApiResponse<Void> updateProgress(
            @Parameter(description = "视频流ID", required = true, example = "ai_stream_12345") @PathVariable String streamId,
            @Parameter(description = "进度百分比", required = true, example = "65") @RequestParam Integer progress,
            @Parameter(description = "状态", required = true, example = "GENERATING") @RequestParam String status
    ) {
        aiVideoService.updateGenerationProgress(streamId, progress, status);
        return ApiResponse.success();
    }
    
    @PostMapping("/presets/music-visualization")
    @Operation(summary = "音乐可视化预设", description = "快速生成与当前播放音乐同步的可视化AI视频")
    public ApiResponse<AiVideoStreamResponse> generateMusicVisualization(
            @Parameter(description = "包间ID", required = true, example = "1001") @RequestParam Long roomId,
            @Parameter(description = "当前播放歌曲ID", example = "12345") @RequestParam(required = false) Long trackId,
            @Parameter(description = "可视化风格", example = "CYBERPUNK") @RequestParam(defaultValue = "MODERN") String style
    ) {
        AiVideoGenerateRequest request = new AiVideoGenerateRequest();
        request.setRoomId(roomId);
        request.setVideoType("MUSIC_VISUALIZATION");
        request.setStyle(style);
        request.setCurrentTrackId(trackId);
        request.setAudioSync(true);
        request.setDuration(180); // 3分钟
        request.setPrompt("根据音乐节拍生成动态视觉效果，" + style.toLowerCase() + "风格");
        
        AiVideoStreamResponse response = aiVideoService.startVideoGeneration(request);
        return ApiResponse.success(response);
    }
    
    @PostMapping("/presets/ambient-scene")
    @Operation(summary = "环境场景预设", description = "生成环境氛围场景AI视频作为背景")
    public ApiResponse<AiVideoStreamResponse> generateAmbientScene(
            @Parameter(description = "包间ID", required = true, example = "1001") @RequestParam Long roomId,
            @Parameter(description = "场景类型", example = "NATURE") @RequestParam(defaultValue = "NATURE") String sceneType,
            @Parameter(description = "视频时长（秒）", example = "300") @RequestParam(defaultValue = "300") Integer duration
    ) {
        AiVideoGenerateRequest request = new AiVideoGenerateRequest();
        request.setRoomId(roomId);
        request.setVideoType("AMBIENT_SCENE");
        request.setStyle(sceneType);
        request.setDuration(duration);
        request.setPrompt(generateScenePrompt(sceneType));
        
        AiVideoStreamResponse response = aiVideoService.startVideoGeneration(request);
        return ApiResponse.success(response);
    }
    
    /**
     * 根据场景类型生成提示词
     */
    private String generateScenePrompt(String sceneType) {
        return switch (sceneType.toUpperCase()) {
            case "NATURE" -> "宁静的自然风光，森林、湖泊、山川，缓慢的镜头移动，4K高清画质";
            case "CYBERPUNK" -> "未来科幻城市，霓虹灯闪烁，高楼大厦，赛博朋克风格，动态光影效果";
            case "ABSTRACT" -> "抽象艺术动画，流动的色彩和几何形状，现代艺术风格";
            case "RETRO" -> "复古怀旧风格，80年代美学，合成器波浪，霓虹色彩";
            case "SPACE" -> "宇宙星空，星云，行星，深空探索，壮丽的宇宙景观";
            default -> "现代简约风格，柔和的色彩过渡，简洁的几何元素";
        };
    }
}
