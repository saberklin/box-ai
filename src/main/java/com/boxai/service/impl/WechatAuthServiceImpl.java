package com.boxai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boxai.auth.JwtService;
import com.boxai.domain.dto.user.UserProfileDTO;
import com.boxai.domain.dto.wechat.LoginResponse;
import com.boxai.domain.dto.wechat.WechatLoginRequest;
import com.boxai.domain.dto.wechat.RoomQrResponse;
import com.boxai.domain.entity.User;
import com.boxai.service.UserService;
import com.boxai.service.WechatAuthService;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class WechatAuthServiceImpl implements WechatAuthService {
    @Value("${app.wechat.appid}")
    private String appid;
    @Value("${app.wechat.secret}")
    private String secret;
    private final WebClient.Builder webClientBuilder;
    private final UserService userService;
    private final JwtService jwtService;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;


    @Override
    public LoginResponse loginSilent(WechatLoginRequest req) {
        String json = webClientBuilder.baseUrl("https://api.weixin.qq.com").build().get()
                .uri(uriBuilder -> uriBuilder.path("/sns/jscode2session")
                        .queryParam("appid", appid)
                        .queryParam("secret", secret)
                        .queryParam("js_code", req.getJsCode())
                        .queryParam("grant_type", "authorization_code")
                        .build())
                .accept(MediaType.ALL)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        Js2SessionResp resp;
        try {
            resp = objectMapper.readValue(json, Js2SessionResp.class);
        } catch (Exception e) {
            throw new RuntimeException("微信返回解析失败: " + json);
        }

        if (resp != null && resp.errcode != null && resp.errcode != 0) {
            throw new RuntimeException("微信登录失败: " + resp.errmsg + " (" + resp.errcode + ")");
        }
        if (resp == null || resp.openid == null) {
            throw new RuntimeException("微信登录失败");
        }

        User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getOpenId, resp.openid));
        if (user == null) {
            user = new User();
            user.setOpenId(resp.openid);
            user.setNickname(req.getNickname());
            user.setAvatarUrl(req.getAvatarUrl());
            userService.save(user);
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("openId", resp.openid);
        String token = jwtService.createToken(user.getId(), claims);
        redis.opsForValue().set("wx:session:" + user.getId(), resp.session_key, 7, TimeUnit.DAYS);
        return new LoginResponse(token, user.getId(),resp.openid);
    }

    @Override
    public UserProfileDTO getCurrentUserProfile(Long userId) {
        User u = userService.getById(userId);
        if (u == null) return null;
        UserProfileDTO dto = new UserProfileDTO();
        BeanUtils.copyProperties(u, dto);
        return dto;
    }

    @Data
    static class Js2SessionResp {
        public String openid;
        public String session_key;
        public String unionid;
        public Integer errcode;
        public String errmsg;
    }
}


