# 简化的AI视频生成API测试
$baseUrl = "http://localhost:9998"

Write-Host "=== AI视频生成服务测试 ===" -ForegroundColor Green

# 1. 健康检查
Write-Host "1. 健康检查..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "$baseUrl/api/health" -Method GET
    Write-Host "✅ 应用状态: OK" -ForegroundColor Green
} catch {
    Write-Host "❌ 应用未启动" -ForegroundColor Red
    exit 1
}

# 2. AI视频生成
Write-Host "2. 测试AI视频生成..." -ForegroundColor Yellow
$body = @{
    roomId = 1001
    videoType = "MUSIC_VISUALIZATION"
    prompt = "科幻城市霓虹灯"
    style = "CYBERPUNK"
    duration = 60
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/ai-video/generate" -Method POST -Body $body -ContentType "application/json"
    
    if ($response.code -eq 0) {
        Write-Host "✅ 生成请求成功" -ForegroundColor Green
        Write-Host "   流ID: $($response.data.streamId)" -ForegroundColor Cyan
        Write-Host "   状态: $($response.data.status)" -ForegroundColor Cyan
        Write-Host "   RTMP: $($response.data.streamUrl)" -ForegroundColor Cyan
        
        $streamId = $response.data.streamId
        
        # 3. 查询状态
        Write-Host "3. 查询状态..." -ForegroundColor Yellow
        Start-Sleep 3
        
        $status = Invoke-RestMethod -Uri "$baseUrl/api/ai-video/status/$streamId" -Method GET
        Write-Host "✅ 当前状态: $($status.data.status)" -ForegroundColor Green
        Write-Host "   当前进度: $($status.data.progress)%" -ForegroundColor Cyan
        
        # 4. 停止生成
        Write-Host "4. 停止生成..." -ForegroundColor Yellow
        $stop = Invoke-RestMethod -Uri "$baseUrl/api/ai-video/stop/$streamId" -Method POST
        Write-Host "✅ 已停止" -ForegroundColor Green
        
    } else {
        Write-Host "❌ 生成失败: $($response.message)" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ API调用失败: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "📖 Swagger文档: $baseUrl/swagger-ui.html" -ForegroundColor Cyan
Write-Host "🔍 实时进度: $baseUrl/api/ai-video/stream/progress/{streamId}" -ForegroundColor Cyan
