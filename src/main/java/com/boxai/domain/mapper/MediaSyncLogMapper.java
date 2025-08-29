package com.boxai.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boxai.domain.entity.MediaSyncLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 媒体同步日志 Mapper 接口
 */
@Mapper
public interface MediaSyncLogMapper extends BaseMapper<MediaSyncLog> {
    
}
