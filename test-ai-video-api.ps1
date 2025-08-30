# AI视频生成API测试脚本
# 测试Box-AI系统的AI视频生成功能

$baseUrl = "http://localhost:9998"

Write-Host "=== Box-AI 视频生成服务测试 ===" -ForegroundColor Green
Write-Host ""

# 1. 测试健康检查
Write-Host "1. 测试应用健康状态..." -ForegroundColor Yellow
try {
    $healthResponse = Invoke-RestMethod -Uri "$baseUrl/api/health" -Method GET
    Write-Host "✅ 应用健康状态: $($healthResponse.status)" -ForegroundColor Green
} catch {
    Write-Host "❌ 健康检查失败: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# 2. 测试AI视频生成接口
Write-Host ""
Write-Host "2. 测试AI视频生成接口..." -ForegroundColor Yellow

$generateRequest = @{
    roomId = 1001
    videoType = "MUSIC_VISUALIZATION"
    prompt = "科幻未来城市，霓虹灯闪烁，配合音乐节拍"
    style = "CYBERPUNK"
    resolution = "1920x1080"
    frameRate = 30
    duration = 60
    audioSync = $true
    currentTrackId = 12345
    priority = "HIGH"
    realtime = $true
}

$generateRequestJson = $generateRequest | ConvertTo-Json

try {
    $generateResponse = Invoke-RestMethod -Uri "$baseUrl/api/ai-video/generate" -Method POST -Body $generateRequestJson -ContentType "application/json"
    
    if ($generateResponse.code -eq 0) {
        Write-Host "✅ AI视频生成请求成功" -ForegroundColor Green
        Write-Host "   流ID: $($generateResponse.data.streamId)" -ForegroundColor Cyan
        Write-Host "   状态: $($generateResponse.data.status)" -ForegroundColor Cyan
        Write-Host "   进度: $($generateResponse.data.progress)%" -ForegroundColor Cyan
        Write-Host "   RTMP流: $($generateResponse.data.streamUrl)" -ForegroundColor Cyan
        Write-Host "   HLS流: $($generateResponse.data.hlsUrl)" -ForegroundColor Cyan
        Write-Host "   WebRTC流: $($generateResponse.data.webrtcUrl)" -ForegroundColor Cyan
        
        $streamId = $generateResponse.data.streamId
        
        # 3. 测试状态查询
        Write-Host ""
        Write-Host "3. 测试状态查询..." -ForegroundColor Yellow
        Start-Sleep -Seconds 2
        
        $statusResponse = Invoke-RestMethod -Uri "$baseUrl/api/ai-video/status/$streamId" -Method GET
        if ($statusResponse.code -eq 0) {
            Write-Host "✅ 状态查询成功" -ForegroundColor Green
            Write-Host "   当前状态: $($statusResponse.data.status)" -ForegroundColor Cyan
            Write-Host "   当前进度: $($statusResponse.data.progress)%" -ForegroundColor Cyan
        }
        
        # 4. 测试预设接口
        Write-Host ""
        Write-Host "4. 测试音乐可视化预设..." -ForegroundColor Yellow
        
        $presetParams = @{
            roomId = 1001
            trackId = 12345
            style = "NATURE"
        }
        
        $presetParamsJson = $presetParams | ConvertTo-Json
        $presetResponse = Invoke-RestMethod -Uri "$baseUrl/api/ai-video/presets/music-visualization" -Method POST -Body $presetParamsJson -ContentType "application/json"
        if ($presetResponse.code -eq 0) {
            Write-Host "✅ 音乐可视化预设成功" -ForegroundColor Green
            Write-Host "   预设流ID: $($presetResponse.data.streamId)" -ForegroundColor Cyan
        }
        
        # 5. 测试环境场景预设
        Write-Host ""
        Write-Host "5. 测试环境场景预设..." -ForegroundColor Yellow
        
        $sceneParams = @{
            roomId = 1001
            sceneType = "NATURE"
            duration = 300
        }
        
        $sceneParamsJson = $sceneParams | ConvertTo-Json
        $sceneResponse = Invoke-RestMethod -Uri "$baseUrl/api/ai-video/presets/ambient-scene" -Method POST -Body $sceneParamsJson -ContentType "application/json"
        if ($sceneResponse.code -eq 0) {
            Write-Host "✅ 环境场景预设成功" -ForegroundColor Green
            Write-Host "   场景流ID: $($sceneResponse.data.streamId)" -ForegroundColor Cyan
        }
        
        # 6. 测试停止生成
        Write-Host ""
        Write-Host "6. 测试停止生成..." -ForegroundColor Yellow
        Start-Sleep -Seconds 2
        
        $stopResponse = Invoke-RestMethod -Uri "$baseUrl/api/ai-video/stop/$streamId" -Method POST
        if ($stopResponse.code -eq 0) {
            Write-Host "✅ 停止生成成功" -ForegroundColor Green
        }
        
    } else {
        Write-Host "❌ AI视频生成失败: $($generateResponse.message)" -ForegroundColor Red
    }
    
} catch {
    Write-Host "❌ API调用失败: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "   请确保应用程序正在运行在 $baseUrl" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== 测试完成 ===" -ForegroundColor Green
Write-Host ""
Write-Host "📖 查看API文档: $baseUrl/swagger-ui.html" -ForegroundColor Cyan
Write-Host "🔍 实时进度流: $baseUrl/api/ai-video/stream/progress/{streamId}" -ForegroundColor Cyan
Write-Host "📊 所有会话状态: $baseUrl/api/ai-video/stream/status" -ForegroundColor Cyan
Write-Host ""
Write-Host "💡 提示: 当前使用的是Mock AI服务，可以在application.yml中配置真实的AI服务" -ForegroundColor Yellow
