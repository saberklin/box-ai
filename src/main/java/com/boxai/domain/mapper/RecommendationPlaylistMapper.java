package com.boxai.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boxai.domain.entity.RecommendationPlaylist;
import org.apache.ibatis.annotations.Mapper;

/**
 * 推荐歌单Mapper接口
 */
@Mapper
public interface RecommendationPlaylistMapper extends BaseMapper<RecommendationPlaylist> {
}
