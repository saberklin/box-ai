package com.boxai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.boxai.domain.entity.UserPlaylist;
import com.boxai.domain.mapper.UserPlaylistMapper;
import com.boxai.service.UserPlaylistService;
import org.springframework.stereotype.Service;

@Service
public class UserPlaylistServiceImpl extends ServiceImpl<UserPlaylistMapper, UserPlaylist> implements UserPlaylistService {
}


