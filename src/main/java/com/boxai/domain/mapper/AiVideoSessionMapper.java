package com.boxai.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boxai.domain.entity.AiVideoSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI视频生成会话Mapper接口
 */
@Mapper
public interface AiVideoSessionMapper extends BaseMapper<AiVideoSession> {
}
