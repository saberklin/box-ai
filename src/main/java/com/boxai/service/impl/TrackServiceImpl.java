package com.boxai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.boxai.domain.entity.Track;
import com.boxai.domain.mapper.TrackMapper;
import com.boxai.service.TrackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class TrackServiceImpl extends ServiceImpl<TrackMapper, Track> implements TrackService {
    
    @Override
    public Page<Track> searchTracks(String keyword, Page<Track> page) {
        log.info("搜索歌曲: keyword={}, page={}, size={}", keyword, page.getCurrent(), page.getSize());
        
        LambdaQueryWrapper<Track> queryWrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                .like(Track::getTitle, keyword)
                .or()
                .like(Track::getArtist, keyword)
                .or()
                .like(Track::getAlbum, keyword)
                .or()
                .like(Track::getTags, keyword)
            );
        }
        
        // 只返回状态为ACTIVE的歌曲
        queryWrapper.eq(Track::getStatus, "ACTIVE");
        
        // 按热度和播放次数排序
        queryWrapper.orderByDesc(Track::getHotScore)
                   .orderByDesc(Track::getPlayCount);
        
        return page(page, queryWrapper);
    }
}


