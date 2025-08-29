package com.boxai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.boxai.domain.entity.Lighting;
import com.boxai.domain.mapper.LightingMapper;
import com.boxai.service.LightingService;
import org.springframework.stereotype.Service;

@Service
public class LightingServiceImpl extends ServiceImpl<LightingMapper, Lighting> implements LightingService {
}


