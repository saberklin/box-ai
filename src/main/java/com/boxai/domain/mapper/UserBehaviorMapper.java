package com.boxai.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boxai.domain.entity.UserBehavior;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 用户行为记录Mapper接口
 */
@Mapper
public interface UserBehaviorMapper extends BaseMapper<UserBehavior> {
    
    /**
     * 统计用户各类型歌曲播放次数
     */
    @Select("SELECT t.category, COUNT(*) as count " +
            "FROM t_user_behavior ub " +
            "JOIN t_track t ON ub.target_id = t.id " +
            "WHERE ub.user_id = #{userId} AND ub.behavior_type = 'PLAY' AND ub.target_type = 'TRACK' " +
            "GROUP BY t.category " +
            "ORDER BY count DESC")
    List<Map<String, Object>> getCategoryPlayCount(@Param("userId") Long userId);
    
    /**
     * 统计用户各时间段活跃度
     */
    @Select("SELECT " +
            "CASE " +
            "  WHEN EXTRACT(HOUR FROM created_at) BETWEEN 6 AND 11 THEN 'morning' " +
            "  WHEN EXTRACT(HOUR FROM created_at) BETWEEN 12 AND 17 THEN 'afternoon' " +
            "  WHEN EXTRACT(HOUR FROM created_at) BETWEEN 18 AND 23 THEN 'evening' " +
            "  ELSE 'night' " +
            "END as time_slot, " +
            "COUNT(*) as count " +
            "FROM t_user_behavior " +
            "WHERE user_id = #{userId} " +
            "GROUP BY time_slot " +
            "ORDER BY count DESC")
    List<Map<String, Object>> getTimeSlotActivity(@Param("userId") Long userId);
    
    /**
     * 获取用户活跃天数
     */
    @Select("SELECT COUNT(DISTINCT DATE(created_at)) as active_days " +
            "FROM t_user_behavior " +
            "WHERE user_id = #{userId}")
    Integer getUserActiveDays(@Param("userId") Long userId);
    
    /**
     * 获取用户最后活跃日期
     */
    @Select("SELECT DATE(MAX(created_at)) as last_active_date " +
            "FROM t_user_behavior " +
            "WHERE user_id = #{userId}")
    LocalDate getUserLastActiveDate(@Param("userId") Long userId);
}
