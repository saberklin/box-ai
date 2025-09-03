package com.boxai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.boxai.domain.dto.user.UserProfileAnalysisDTO;
import com.boxai.domain.entity.User;
import com.boxai.domain.entity.UserBehavior;
import com.boxai.domain.entity.UserProfile;
import com.boxai.domain.mapper.UserBehaviorMapper;
import com.boxai.domain.mapper.UserProfileMapper;
import com.boxai.service.UserBehaviorService;
import com.boxai.service.UserProfileService;
import com.boxai.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户画像服务实现
 */
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl extends ServiceImpl<UserProfileMapper, UserProfile> implements UserProfileService {
    
    private final UserBehaviorService userBehaviorService;
    private final UserBehaviorMapper userBehaviorMapper;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    @Override
    public UserProfileAnalysisDTO getUserProfileAnalysis(Long userId) {
        // 获取用户基本信息
        User user = userService.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 获取或创建用户画像
        UserProfile profile = getOne(new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId));
        if (profile == null) {
            updateUserProfile(userId);
            profile = getOne(new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId));
        }
        
        UserProfileAnalysisDTO dto = new UserProfileAnalysisDTO();
        dto.setUserId(userId);
        dto.setNickname(user.getNickname());
        dto.setLastActiveDate(profile.getLastActiveDate());
        dto.setAvgSessionDuration(profile.getAvgSessionDuration());
        
        // 解析偏好类型
        dto.setFavoriteCategories(parseJsonToMap(profile.getFavoriteCategories()));
        
        // 解析偏好时间段
        dto.setPreferredTimeSlots(parseJsonToMap(profile.getPreferredTimeSlots()));
        
        // 构建使用频次统计
        UserProfileAnalysisDTO.UsageFrequency frequency = new UserProfileAnalysisDTO.UsageFrequency();
        frequency.setTotalPlayCount(profile.getTotalPlayCount());
        frequency.setTotalLikeCount(profile.getTotalLikeCount());
        frequency.setTotalSearchCount(profile.getTotalSearchCount());
        frequency.setActiveDays(profile.getActiveDays());
        
        // 计算日均播放次数
        if (profile.getActiveDays() > 0) {
            frequency.setAvgDailyPlays((double) profile.getTotalPlayCount() / profile.getActiveDays());
        } else {
            frequency.setAvgDailyPlays(0.0);
        }
        
        // 计算活跃度等级
        frequency.setActivityLevel(calculateActivityLevel(profile.getTotalPlayCount(), profile.getActiveDays()));
        dto.setUsageFrequency(frequency);
        
        // 生成用户标签
        dto.setUserTags(generateUserTags(dto));
        
        return dto;
    }

    @Override
    public void updateUserProfile(Long userId) {
        // 获取或创建用户画像
        UserProfile profile = getOne(new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId));
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(userId);
        }
        
        // 统计各类型歌曲播放次数
        List<Map<String, Object>> categoryStats = userBehaviorMapper.getCategoryPlayCount(userId);
        Map<String, Integer> categoryMap = new HashMap<>();
        for (Map<String, Object> stat : categoryStats) {
            String category = (String) stat.get("category");
            Integer count = ((Number) stat.get("count")).intValue();
            if (category != null) {
                categoryMap.put(category, count);
            }
        }
        profile.setFavoriteCategories(mapToJson(categoryMap));
        
        // 统计时间段偏好
        List<Map<String, Object>> timeSlotStats = userBehaviorMapper.getTimeSlotActivity(userId);
        Map<String, Integer> timeSlotMap = new HashMap<>();
        for (Map<String, Object> stat : timeSlotStats) {
            String timeSlot = (String) stat.get("time_slot");
            Integer count = ((Number) stat.get("count")).intValue();
            timeSlotMap.put(timeSlot, count);
        }
        profile.setPreferredTimeSlots(mapToJson(timeSlotMap));
        
        // 统计总数
        profile.setTotalPlayCount(countBehavior(userId, UserBehavior.BehaviorType.PLAY));
        profile.setTotalLikeCount(countBehavior(userId, UserBehavior.BehaviorType.LIKE));
        profile.setTotalSearchCount(countBehavior(userId, UserBehavior.BehaviorType.SEARCH));
        
        // 获取活跃天数和最后活跃日期
        profile.setActiveDays(userBehaviorMapper.getUserActiveDays(userId));
        profile.setLastActiveDate(userBehaviorMapper.getUserLastActiveDate(userId));
        
        // 计算平均会话时长（简化实现）
        profile.setAvgSessionDuration(calculateAvgSessionDuration(userId));
        
        saveOrUpdate(profile);
    }

    @Override
    public void recordUserBehavior(Long userId, String behaviorType, Long targetId, String targetType, String metadata, String sessionId) {
        UserBehavior behavior = new UserBehavior();
        behavior.setUserId(userId);
        behavior.setBehaviorType(behaviorType);
        behavior.setTargetId(targetId);
        behavior.setTargetType(targetType);
        behavior.setMetadata(metadata);
        behavior.setSessionId(sessionId);
        behavior.setCreatedAt(OffsetDateTime.now());
        
        userBehaviorService.save(behavior);
        
        // 异步更新用户画像（简化实现，直接同步更新）
        updateUserProfile(userId);
    }
    
    private Integer countBehavior(Long userId, String behaviorType) {
        return Math.toIntExact(userBehaviorService.count(new LambdaQueryWrapper<UserBehavior>()
                .eq(UserBehavior::getUserId, userId)
                .eq(UserBehavior::getBehaviorType, behaviorType)));
    }
    
    private Integer calculateAvgSessionDuration(Long userId) {
        try {
            // 获取用户的所有行为记录，按会话ID分组
            List<UserBehavior> behaviors = userBehaviorService.list(
                new LambdaQueryWrapper<UserBehavior>()
                    .eq(UserBehavior::getUserId, userId)
                    .isNotNull(UserBehavior::getSessionId)
                    .orderByAsc(UserBehavior::getCreatedAt)
            );
            
            if (behaviors.isEmpty()) {
                return 0;
            }
            
            // 按会话ID分组
            Map<String, List<UserBehavior>> sessionGroups = behaviors.stream()
                .collect(Collectors.groupingBy(UserBehavior::getSessionId));
            
            List<Integer> sessionDurations = new ArrayList<>();
            
            for (Map.Entry<String, List<UserBehavior>> entry : sessionGroups.entrySet()) {
                List<UserBehavior> sessionBehaviors = entry.getValue();
                if (sessionBehaviors.size() < 2) {
                    continue; // 单个行为无法计算会话时长
                }
                
                // 获取会话的开始和结束时间
                OffsetDateTime startTime = sessionBehaviors.get(0).getCreatedAt();
                OffsetDateTime endTime = sessionBehaviors.get(sessionBehaviors.size() - 1).getCreatedAt();
                
                // 计算会话时长（分钟）
                long durationMinutes = java.time.Duration.between(startTime, endTime).toMinutes();
                
                // 过滤异常长的会话（超过4小时的可能是异常数据）
                if (durationMinutes > 0 && durationMinutes <= 240) {
                    sessionDurations.add((int) durationMinutes);
                }
            }
            
            if (sessionDurations.isEmpty()) {
                return 30; // 默认30分钟
            }
            
            // 计算平均会话时长
            double avgDuration = sessionDurations.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(30.0);
            
            return (int) Math.round(avgDuration);
            
        } catch (Exception e) {
            log.error("计算平均会话时长失败: userId=" + userId, e);
            return 30; // 出错时返回默认值
        }
    }
    
    private String calculateActivityLevel(Integer totalPlays, Integer activeDays) {
        if (totalPlays == null || activeDays == null || activeDays == 0) {
            return "LOW";
        }
        
        double avgDaily = (double) totalPlays / activeDays;
        if (avgDaily >= 20) return "SUPER";
        if (avgDaily >= 10) return "HIGH";
        if (avgDaily >= 5) return "MEDIUM";
        return "LOW";
    }
    
    private UserProfileAnalysisDTO.UserTags generateUserTags(UserProfileAnalysisDTO dto) {
        UserProfileAnalysisDTO.UserTags tags = new UserProfileAnalysisDTO.UserTags();
        
        // 音乐偏好标签
        Map<String, Integer> categories = dto.getFavoriteCategories();
        if (categories != null && !categories.isEmpty()) {
            String topCategory = categories.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("未知");
            tags.setMusicPreference(topCategory + "爱好者");
        }
        
        // 活跃度标签
        String activityLevel = dto.getUsageFrequency().getActivityLevel();
        switch (activityLevel) {
            case "SUPER" -> tags.setActivityTag("超级活跃用户");
            case "HIGH" -> tags.setActivityTag("高活跃用户");
            case "MEDIUM" -> tags.setActivityTag("中等活跃用户");
            default -> tags.setActivityTag("轻度用户");
        }
        
        // 使用习惯标签
        Integer totalPlays = dto.getUsageFrequency().getTotalPlayCount();
        Integer totalLikes = dto.getUsageFrequency().getTotalLikeCount();
        if (totalLikes != null && totalPlays != null && totalPlays > 0) {
            double likeRate = (double) totalLikes / totalPlays;
            if (likeRate > 0.3) {
                tags.setUsageHabit("互动积极型");
            } else if (likeRate > 0.1) {
                tags.setUsageHabit("适度互动型");
            } else {
                tags.setUsageHabit("纯听歌型");
            }
        }
        
        // 时间偏好标签
        Map<String, Integer> timeSlots = dto.getPreferredTimeSlots();
        if (timeSlots != null && !timeSlots.isEmpty()) {
            String topTimeSlot = timeSlots.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("unknown");
            switch (topTimeSlot) {
                case "morning" -> tags.setTimePreference("晨间音乐人");
                case "afternoon" -> tags.setTimePreference("午后悠闲派");
                case "evening" -> tags.setTimePreference("黄昏音乐家");
                case "night" -> tags.setTimePreference("夜猫子歌手");
                default -> tags.setTimePreference("随性听歌族");
            }
        }
        
        return tags;
    }
    
    private String mapToJson(Map<String, Integer> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
    
    private Map<String, Integer> parseJsonToMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Integer>>() {});
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }
}
