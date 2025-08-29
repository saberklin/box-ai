package com.boxai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.boxai.domain.entity.UserProfile;
import com.boxai.domain.dto.user.UserProfileAnalysisDTO;

/**
 * 用户画像服务接口
 * 提供用户画像数据的CRUD操作和分析功能
 */
public interface UserProfileService extends IService<UserProfile> {
    
    /**
     * 获取用户画像分析
     * 包含偏好歌曲类型、使用频次等详细分析
     * @param userId 用户ID
     * @return 用户画像分析结果
     */
    UserProfileAnalysisDTO getUserProfileAnalysis(Long userId);
    
    /**
     * 更新用户画像
     * 基于用户行为数据重新计算并更新用户画像
     * @param userId 用户ID
     */
    void updateUserProfile(Long userId);
    
    /**
     * 记录用户行为
     * 记录用户的播放、点赞、搜索等行为，用于画像分析
     * @param userId 用户ID
     * @param behaviorType 行为类型
     * @param targetId 目标ID
     * @param targetType 目标类型
     * @param metadata 元数据
     * @param sessionId 会话ID
     */
    void recordUserBehavior(Long userId, String behaviorType, Long targetId, String targetType, String metadata, String sessionId);
}
