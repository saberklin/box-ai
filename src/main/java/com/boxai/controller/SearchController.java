package com.boxai.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.boxai.auth.JwtAuthFilter;
import com.boxai.common.web.ApiResponse;
import com.boxai.domain.entity.SearchLog;
import com.boxai.domain.entity.Track;
import com.boxai.service.SearchLogService;
import com.boxai.service.TrackService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 搜索控制器
 * 提供音乐搜索和热门关键词功能
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "搜索功能", description = "音乐搜索和热门关键词相关API")
public class SearchController {
    private final TrackService trackService;
    private final SearchLogService searchLogService;



    /**
     * 搜索音乐曲目
     * 根据关键词搜索曲目，并记录搜索日志
     * @param keyword 搜索关键词
     * @return 匹配的曲目列表
     */
    @GetMapping
    public ApiResponse<List<Track>> search(@RequestParam String keyword) {
        // 写入搜索日志
        Long uid = JwtAuthFilter.CURRENT_USER.get();
        SearchLog log = new SearchLog();
        log.setUserId(uid);
        log.setKeyword(keyword);
        searchLogService.save(log);

        List<Track> list = trackService.list(Wrappers.<Track>lambdaQuery()
                .like(Track::getTitle, keyword).or().like(Track::getArtist, keyword).or().like(Track::getTags, keyword));
        return ApiResponse.ok(list);
    }

    /**
     * 获取热门搜索关键词
     * 根据搜索频次返回前10个热门关键词
     * @return 热门关键词列表（按搜索次数降序）
     */
    @GetMapping("/hot")
    public ApiResponse<List<String>> hotKeywords() {
        List<String> hot = searchLogService.list(Wrappers.<SearchLog>lambdaQuery())
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(SearchLog::getKeyword, java.util.stream.Collectors.counting()))
                .entrySet().stream().sorted((a,b)-> Long.compare(b.getValue(), a.getValue()))
                .limit(10).map(java.util.Map.Entry::getKey).toList();
        return ApiResponse.ok(hot);
    }
}


