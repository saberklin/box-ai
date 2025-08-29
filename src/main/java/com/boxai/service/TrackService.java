package com.boxai.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.boxai.domain.entity.Track;

/**
 * 音乐曲目服务接口
 * 提供曲目信息的CRUD操作和业务逻辑
 */
public interface TrackService extends IService<Track> {
    
    /**
     * 搜索歌曲
     * @param keyword 搜索关键词
     * @param page 分页参数
     * @return 搜索结果
     */
    Page<Track> searchTracks(String keyword, Page<Track> page);
}


