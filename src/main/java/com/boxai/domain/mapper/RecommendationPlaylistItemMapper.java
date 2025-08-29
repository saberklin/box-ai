package com.boxai.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boxai.domain.entity.RecommendationPlaylistItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 推荐歌单曲目Mapper接口
 */
@Mapper
public interface RecommendationPlaylistItemMapper extends BaseMapper<RecommendationPlaylistItem> {
}
