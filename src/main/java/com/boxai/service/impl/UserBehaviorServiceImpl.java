package com.boxai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.boxai.domain.entity.UserBehavior;
import com.boxai.domain.mapper.UserBehaviorMapper;
import com.boxai.service.UserBehaviorService;
import org.springframework.stereotype.Service;

/**
 * 用户行为记录服务实现
 */
@Service
public class UserBehaviorServiceImpl extends ServiceImpl<UserBehaviorMapper, UserBehavior> implements UserBehaviorService {
}
