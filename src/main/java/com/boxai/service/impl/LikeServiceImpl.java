package com.boxai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.boxai.domain.entity.LikeRecord;
import com.boxai.domain.mapper.LikeMapper;
import com.boxai.service.LikeService;
import org.springframework.stereotype.Service;

@Service
public class LikeServiceImpl extends ServiceImpl<LikeMapper, LikeRecord> implements LikeService {
}


