package com.boxai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.boxai.domain.entity.SearchLog;
import com.boxai.domain.mapper.SearchLogMapper;
import com.boxai.service.SearchLogService;
import org.springframework.stereotype.Service;

@Service
public class SearchLogServiceImpl extends ServiceImpl<SearchLogMapper, SearchLog> implements SearchLogService {
}


