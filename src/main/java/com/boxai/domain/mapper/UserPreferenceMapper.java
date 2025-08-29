package com.boxai.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boxai.domain.entity.UserPreference;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户偏好标签Mapper接口
 */
@Mapper
public interface UserPreferenceMapper extends BaseMapper<UserPreference> {
}
