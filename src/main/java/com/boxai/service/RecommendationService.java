package com.boxai.service;

import com.boxai.domain.dto.TrackRecommendationDto;
import com.boxai.domain.dto.PlaylistRecommendationDto;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 推荐系统服务接口
 * 提供歌曲和歌单的智能推荐功能
 */
public interface RecommendationService {
    
    /**
     * 获取个性化推荐歌曲
     * 基于用户历史行为和偏好进行推荐
     * 
     * @param userId 用户ID
     * @param limit 推荐数量限制
     * @return 推荐歌曲列表
     */
    List<TrackRecommendationDto> getPersonalizedRecommendations(Long userId, Integer limit);
    
    /**
     * 获取热门推荐歌曲
     * 基于全局热度进行推荐
     * 
     * @param category 分类筛选（可为null）
     * @param limit 推荐数量限制
     * @return 热门歌曲列表
     */
    List<TrackRecommendationDto> getHotRecommendations(String category, Integer limit);
    
    /**
     * 获取新歌推荐
     * 推荐最新上线的歌曲
     * 
     * @param limit 推荐数量限制
     * @return 新歌列表
     */
    List<TrackRecommendationDto> getNewSongRecommendations(Integer limit);
    
    /**
     * 获取相似歌曲推荐
     * 基于指定歌曲推荐相似的歌曲
     * 
     * @param trackId 基准歌曲ID
     * @param limit 推荐数量限制
     * @return 相似歌曲列表
     */
    List<TrackRecommendationDto> getSimilarTracks(Long trackId, Integer limit);
    
    /**
     * 获取推荐歌单列表
     * 返回系统生成的各类推荐歌单
     * 
     * @return 推荐歌单列表
     */
    List<PlaylistRecommendationDto> getRecommendationPlaylists();
    
    /**
     * 获取指定推荐歌单的详细信息
     * 
     * @param playlistId 歌单ID
     * @param page 分页参数
     * @return 歌单详情和歌曲列表
     */
    Page<TrackRecommendationDto> getPlaylistTracks(Long playlistId, Page<TrackRecommendationDto> page);
    
    /**
     * 获取热门榜单
     * 
     * @param rankingType 榜单类型（DAILY/WEEKLY/MONTHLY）
     * @param category 分类筛选（可为null）
     * @param limit 榜单数量限制
     * @return 热门榜单
     */
    List<TrackRecommendationDto> getHotRanking(String rankingType, String category, Integer limit);
    
    /**
     * 更新用户偏好
     * 基于用户行为（播放、点赞、收藏等）更新偏好模型
     * 
     * @param userId 用户ID
     * @param trackId 歌曲ID
     * @param action 行为类型（PLAY/LIKE/SKIP等）
     */
    void updateUserPreference(Long userId, Long trackId, String action);
    
    /**
     * 刷新热门榜单
     * 定时任务调用，重新计算并更新热门榜单
     */
    void refreshHotRankings();
    
    /**
     * 刷新推荐歌单
     * 定时任务调用，重新生成推荐歌单
     */
    void refreshRecommendationPlaylists();
}
