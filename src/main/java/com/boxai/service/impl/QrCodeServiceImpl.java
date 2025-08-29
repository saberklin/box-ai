package com.boxai.service.impl;

import com.boxai.domain.dto.wechat.RoomQrResponse;
import com.boxai.service.QrCodeService;
import com.boxai.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

/**
 * 二维码生成服务实现
 * 负责微信小程序二维码的生成、缓存和版本管理
 */
@Service
@RequiredArgsConstructor
public class QrCodeServiceImpl implements QrCodeService {
    @Value("${app.wechat.appid}")
    private String appid;
    @Value("${app.wechat.secret}")
    private String secret;
    private final WebClient.Builder webClientBuilder;
    private final StringRedisTemplate redis;


    @Override
    public RoomQrResponse generateRoomQr(Long roomId, String page) {
        // 默认使用版本1
        return generateRoomQrWithVersion(roomId, 1, page);
    }
    
    @Override
    public RoomQrResponse generateRoomQrWithVersion(Long roomId, Integer version, String page) {
        if (roomId == null || version == null) {
            throw new RuntimeException("房间ID和版本号不能为空");
        }
        
        // 检查缓存
        String cacheKey = "wx:room_qr:" + roomId + ":v" + version;
        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null) {
            return new RoomQrResponse(roomId, cached);
        }
        
        String accessToken = getAccessTokenWithCache();

        // 生成小程序码（场景参数包含roomId和版本号）
        Map<String, Object> body = new HashMap<>();
        body.put("scene", "room=" + roomId + "&v=" + version);
        body.put("page", page != null ? page : "pages/index/index");
        body.put("check_path", false);
        body.put("width", 430);

        byte[] png = retry(3, 200L, () -> webClientBuilder.build().post()
                .uri("https://api.weixin.qq.com/wxa/getwxacodeunlimit?access_token=" + accessToken)
                .bodyValue(body)
                .retrieve().bodyToMono(byte[].class).block());

        String base64 = "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(png);
        
        // 缓存二维码（30分钟）
        redis.opsForValue().set(cacheKey, base64, java.time.Duration.ofMinutes(30));
        
        return new RoomQrResponse(roomId, base64);
    }
    


    private String getAccessTokenWithCache() {
        String key = "wx:access_token";
        String cached = redis.opsForValue().get(key);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }
        Map<?, ?> tokenMap = retry(3, 200L, () -> webClientBuilder.baseUrl("https://api.weixin.qq.com").build()
                .get()
                .uri(uri -> uri.path("/cgi-bin/token").queryParam("grant_type","client_credential")
                        .queryParam("appid", appid).queryParam("secret", secret).build())
                .retrieve().bodyToMono(Map.class).block());
        String accessToken = tokenMap != null ? (String) tokenMap.get("access_token") : null;
        Number expiresIn = tokenMap != null ? (Number) tokenMap.get("expires_in") : null;
        if (accessToken == null) {
            throw new RuntimeException("获取access_token失败");
        }
        long ttl = expiresIn != null ? Math.max(1, expiresIn.longValue() - 120) : 660L; // 提前2分钟失效
        redis.opsForValue().set(key, accessToken, java.time.Duration.ofSeconds(ttl));
        return accessToken;
    }

    private <T> T retry(int times, long backoffMs, java.util.concurrent.Callable<T> call) {
        RuntimeException last = null;
        for (int i = 0; i < times; i++) {
            try {
                return call.call();
            } catch (Exception e) {
                last = e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
                try { Thread.sleep(backoffMs); } catch (InterruptedException ignored) {}
            }
        }
        throw last != null ? last : new RuntimeException("未知重试错误");
    }
}
