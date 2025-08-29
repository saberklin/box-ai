package com.boxai.service;

import com.boxai.domain.dto.user.UserProfileDTO;
import com.boxai.domain.dto.wechat.LoginResponse;
import com.boxai.domain.dto.wechat.WechatLoginRequest;
import com.boxai.domain.dto.wechat.RoomQrResponse;

/**
 * 微信认证服务接口
 * 提供微信登录、用户信息获取和小程序码生成功能
 */
public interface WechatAuthService {
    /**
     * 微信静默登录
     * 通过微信jsCode进行用户身份验证，创建或更新用户信息，返回JWT令牌
     * @param req 登录请求（包含jsCode、昵称、头像等信息）
     * @return 登录响应（包含JWT令牌和用户ID）
     */
    LoginResponse loginSilent(WechatLoginRequest req);

    /**
     * 获取用户详细资料
     * @param userId 用户ID
     * @return 用户详细信息（昵称、头像、创建时间等）
     */
    UserProfileDTO getCurrentUserProfile(Long userId);


}


