# Redis消息测试脚本
Write-Host "🧪 开始Redis消息测试..."

# 等待后端启动
Write-Host "⏳ 等待后端启动..."
Start-Sleep -Seconds 10

# 测试后端健康检查
Write-Host "🔍 检查后端状态..."
try {
    $response = Invoke-WebRequest -Uri "http://localhost:9998/api/health" -Method GET -TimeoutSec 5
    Write-Host "✅ 后端运行正常: $($response.StatusCode)"
} catch {
    Write-Host "❌ 后端未启动: $($_.Exception.Message)"
    exit 1
}

# 发送设备控制消息测试
Write-Host "📡 发送设备控制消息..."
try {
    $body = @{
        roomId = 1001
        action = "PLAY"
        trackId = 1
        userId = 1
    } | ConvertTo-Json

    $response = Invoke-WebRequest -Uri "http://localhost:9998/api/playback/control" -Method POST -Body $body -ContentType "application/json" -TimeoutSec 10
    Write-Host "✅ 播放控制消息发送成功: $($response.StatusCode)"
} catch {
    Write-Host "⚠️  播放控制消息发送失败: $($_.Exception.Message)"
}

# 发送灯光控制消息测试
Write-Host "💡 发送灯光控制消息..."
try {
    $body = @{
        roomId = 1001
        brightness = 80
        color = "#FF0000"
        rhythm = "NORMAL"
    } | ConvertTo-Json

    $response = Invoke-WebRequest -Uri "http://localhost:9998/api/lighting/save" -Method POST -Body $body -ContentType "application/json" -TimeoutSec 10
    Write-Host "✅ 灯光控制消息发送成功: $($response.StatusCode)"
} catch {
    Write-Host "⚠️  灯光控制消息发送失败: $($_.Exception.Message)"
}

# 直接通过Redis CLI发送消息测试
Write-Host "🔄 通过Redis CLI直接发送测试消息..."

# 发送设备控制消息
$deviceMessage = '{"roomId":1001,"action":"PAUSE","trackId":2,"userId":1,"timestamp":' + [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds() + '}'
& redis-cli PUBLISH "device:control" $deviceMessage
Write-Host "📨 设备控制消息已发送: $deviceMessage"

# 发送灯光控制消息
$lightMessage = '{"roomId":1001,"brightness":60,"color":"#00FF00","rhythm":"SOFT","timestamp":' + [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds() + '}'
& redis-cli PUBLISH "device:light" $lightMessage
Write-Host "💡 灯光控制消息已发送: $lightMessage"

Write-Host "🎉 测试完成！请检查JavaFX客户端是否收到消息。"
Write-Host "📋 如果客户端正在运行，应该能看到控制台输出或界面变化。"
