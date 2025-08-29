package com.boxai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.boxai.domain.entity.Playlist;
import com.boxai.domain.mapper.PlaylistMapper;
import com.boxai.service.PlaylistService;
import org.springframework.stereotype.Service;

@Service
public class PlaylistServiceImpl extends ServiceImpl<PlaylistMapper, Playlist> implements PlaylistService {
}


