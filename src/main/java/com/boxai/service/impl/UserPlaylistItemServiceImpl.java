package com.boxai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.boxai.domain.entity.UserPlaylistItem;
import com.boxai.domain.mapper.UserPlaylistItemMapper;
import com.boxai.service.UserPlaylistItemService;
import org.springframework.stereotype.Service;

@Service
public class UserPlaylistItemServiceImpl extends ServiceImpl<UserPlaylistItemMapper, UserPlaylistItem> implements UserPlaylistItemService {
}


