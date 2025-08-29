package com.boxai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.boxai.domain.entity.Scene;
import com.boxai.domain.mapper.SceneMapper;
import com.boxai.service.SceneService;
import org.springframework.stereotype.Service;

@Service
public class SceneServiceImpl extends ServiceImpl<SceneMapper, Scene> implements SceneService {
}


