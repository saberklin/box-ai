package com.boxai.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boxai.domain.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}


