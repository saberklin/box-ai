package com.boxai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.boxai.domain.entity.PlaybackHistory;
import com.boxai.domain.mapper.PlaybackHistoryMapper;
import com.boxai.service.PlaybackHistoryService;
import org.springframework.stereotype.Service;

@Service
public class PlaybackHistoryServiceImpl extends ServiceImpl<PlaybackHistoryMapper, PlaybackHistory> implements PlaybackHistoryService {
}
