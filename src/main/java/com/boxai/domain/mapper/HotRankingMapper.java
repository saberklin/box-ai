package com.boxai.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boxai.domain.entity.HotRanking;
import org.apache.ibatis.annotations.Mapper;

/**
 * 热门榜单Mapper接口
 */
@Mapper
public interface HotRankingMapper extends BaseMapper<HotRanking> {
}
