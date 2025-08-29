package com.boxai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boxai.domain.dto.TrackRecommendationDto;
import com.boxai.domain.dto.PlaylistRecommendationDto;
import com.boxai.domain.entity.*;
import com.boxai.domain.mapper.*;
import com.boxai.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 推荐系统服务实现类
 * 实现基于用户行为和内容的混合推荐算法
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {
    
    private final TrackMapper trackMapper;
    private final UserPreferenceMapper userPreferenceMapper;
    private final PlaybackHistoryMapper playbackHistoryMapper;
    private final LikeMapper likeMapper;
    private final RecommendationPlaylistMapper recommendationPlaylistMapper;
    private final RecommendationPlaylistItemMapper recommendationPlaylistItemMapper;
    private final HotRankingMapper hotRankingMapper;
    
    @Override
    public List<TrackRecommendationDto> getPersonalizedRecommendations(Long userId, Integer limit) {
        log.info("获取用户 {} 的个性化推荐，数量限制: {}", userId, limit);
        
        // 1. 获取用户偏好
        List<UserPreference> preferences = userPreferenceMapper.selectList(
            new LambdaQueryWrapper<UserPreference>()
                .eq(UserPreference::getUserId, userId)
                .orderByDesc(UserPreference::getScore)
                .last("LIMIT 10")
        );
        
        if (preferences.isEmpty()) {
            // 新用户，返回热门推荐
            return getHotRecommendations(null, limit);
        }
        
        // 2. 基于偏好生成推荐
        List<Track> recommendedTracks = new ArrayList<>();
        
        for (UserPreference preference : preferences) {
            List<Track> tracks = findTracksByPreference(preference, limit / preferences.size() + 1);
            recommendedTracks.addAll(tracks);
        }
        
        // 3. 去重并按推荐分数排序
        List<Track> uniqueTracks = recommendedTracks.stream()
            .collect(Collectors.toMap(Track::getId, track -> track, (existing, replacement) -> existing))
            .values()
            .stream()
            .sorted((t1, t2) -> Integer.compare(t2.getHotScore(), t1.getHotScore()))
            .limit(limit)
            .collect(Collectors.toList());
        
        // 4. 转换为DTO
        return uniqueTracks.stream()
            .map(track -> convertToRecommendationDto(track, "基于您的听歌偏好", calculateRecommendScore(track, preferences)))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<TrackRecommendationDto> getHotRecommendations(String category, Integer limit) {
        log.info("获取热门推荐，分类: {}, 数量限制: {}", category, limit);
        
        LambdaQueryWrapper<Track> queryWrapper = new LambdaQueryWrapper<Track>()
            .eq(Track::getStatus, "ACTIVE")
            .orderByDesc(Track::getHotScore)
            .orderByDesc(Track::getPlayCount)
            .last("LIMIT " + limit);
        
        if (category != null && !category.isEmpty()) {
            queryWrapper.eq(Track::getCategory, category);
        }
        
        List<Track> hotTracks = trackMapper.selectList(queryWrapper);
        
        return hotTracks.stream()
            .map(track -> convertToRecommendationDto(track, "全站热门歌曲", (double) track.getHotScore() / 100.0))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<TrackRecommendationDto> getNewSongRecommendations(Integer limit) {
        log.info("获取新歌推荐，数量限制: {}", limit);
        
        List<Track> newTracks = trackMapper.selectList(
            new LambdaQueryWrapper<Track>()
                .eq(Track::getStatus, "ACTIVE")
                .eq(Track::getIsNew, true)
                .orderByDesc(Track::getCreatedAt)
                .last("LIMIT " + limit)
        );
        
        return newTracks.stream()
            .map(track -> convertToRecommendationDto(track, "最新上线歌曲", 0.8))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<TrackRecommendationDto> getSimilarTracks(Long trackId, Integer limit) {
        log.info("获取相似歌曲推荐，基准歌曲ID: {}, 数量限制: {}", trackId, limit);
        
        Track baseTrack = trackMapper.selectById(trackId);
        if (baseTrack == null) {
            return Collections.emptyList();
        }
        
        // 基于相同歌手、分类、风格查找相似歌曲
        List<Track> similarTracks = trackMapper.selectList(
            new LambdaQueryWrapper<Track>()
                .eq(Track::getStatus, "ACTIVE")
                .ne(Track::getId, trackId)
                .and(wrapper -> wrapper
                    .eq(Track::getArtist, baseTrack.getArtist())
                    .or()
                    .eq(Track::getCategory, baseTrack.getCategory())
                    .or()
                    .eq(Track::getGenre, baseTrack.getGenre())
                )
                .orderByDesc(Track::getHotScore)
                .last("LIMIT " + limit)
        );
        
        return similarTracks.stream()
            .map(track -> {
                String reason = buildSimilarReason(baseTrack, track);
                double score = calculateSimilarityScore(baseTrack, track);
                return convertToRecommendationDto(track, reason, score);
            })
            .collect(Collectors.toList());
    }
    
    @Override
    public List<PlaylistRecommendationDto> getRecommendationPlaylists() {
        log.info("获取推荐歌单列表");
        
        List<RecommendationPlaylist> playlists = recommendationPlaylistMapper.selectList(
            new LambdaQueryWrapper<RecommendationPlaylist>()
                .eq(RecommendationPlaylist::getIsActive, true)
                .orderByDesc(RecommendationPlaylist::getSortOrder)
                .orderByDesc(RecommendationPlaylist::getPlayCount)
        );
        
        return playlists.stream()
            .map(this::convertToPlaylistDto)
            .collect(Collectors.toList());
    }
    
    @Override
    public Page<TrackRecommendationDto> getPlaylistTracks(Long playlistId, Page<TrackRecommendationDto> page) {
        log.info("获取歌单 {} 的歌曲列表", playlistId);
        
        // 查询歌单信息
        RecommendationPlaylist playlist = recommendationPlaylistMapper.selectById(playlistId);
        if (playlist == null || !playlist.getIsActive()) {
            return new Page<>();
        }
        
        // 查询歌单中的歌曲
        List<RecommendationPlaylistItem> items = recommendationPlaylistItemMapper.selectList(
            new LambdaQueryWrapper<RecommendationPlaylistItem>()
                .eq(RecommendationPlaylistItem::getPlaylistId, playlistId)
                .orderByAsc(RecommendationPlaylistItem::getPosition)
        );
        
        // 分页处理
        int start = (int) ((page.getCurrent() - 1) * page.getSize());
        int end = Math.min(start + (int) page.getSize(), items.size());
        List<RecommendationPlaylistItem> pageItems = items.subList(start, end);
        
        // 获取歌曲详情
        List<Long> trackIds = pageItems.stream()
            .map(RecommendationPlaylistItem::getTrackId)
            .collect(Collectors.toList());
        
        if (trackIds.isEmpty()) {
            return new Page<>();
        }
        
        List<Track> tracks = trackMapper.selectBatchIds(trackIds);
        Map<Long, Track> trackMap = tracks.stream()
            .collect(Collectors.toMap(Track::getId, track -> track));
        
        List<TrackRecommendationDto> trackDtos = pageItems.stream()
            .map(item -> {
                Track track = trackMap.get(item.getTrackId());
                if (track != null) {
                    return convertToRecommendationDto(track, playlist.getName(), item.getWeight());
                }
                return null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        Page<TrackRecommendationDto> result = new Page<>(page.getCurrent(), page.getSize());
        result.setRecords(trackDtos);
        result.setTotal(items.size());
        
        return result;
    }
    
    @Override
    public List<TrackRecommendationDto> getHotRanking(String rankingType, String category, Integer limit) {
        log.info("获取热门榜单，类型: {}, 分类: {}, 数量限制: {}", rankingType, category, limit);
        
        LambdaQueryWrapper<HotRanking> queryWrapper = new LambdaQueryWrapper<HotRanking>()
            .eq(HotRanking::getRankingType, rankingType)
            .orderByAsc(HotRanking::getRankPosition)
            .last("LIMIT " + limit);
        
        if (category != null && !category.isEmpty()) {
            queryWrapper.eq(HotRanking::getCategory, category);
        }
        
        List<HotRanking> rankings = hotRankingMapper.selectList(queryWrapper);
        
        // 获取歌曲详情
        List<Long> trackIds = rankings.stream()
            .map(HotRanking::getTrackId)
            .collect(Collectors.toList());
        
        if (trackIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Track> tracks = trackMapper.selectBatchIds(trackIds);
        Map<Long, Track> trackMap = tracks.stream()
            .collect(Collectors.toMap(Track::getId, track -> track));
        
        return rankings.stream()
            .map(ranking -> {
                Track track = trackMap.get(ranking.getTrackId());
                if (track != null) {
                    String reason = String.format("%s榜单第%d名", getRankingTypeName(rankingType), ranking.getRankPosition());
                    return convertToRecommendationDto(track, reason, ranking.getScore() / 100.0);
                }
                return null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public void updateUserPreference(Long userId, Long trackId, String action) {
        log.info("更新用户 {} 对歌曲 {} 的偏好，行为: {}", userId, trackId, action);
        
        Track track = trackMapper.selectById(trackId);
        if (track == null) {
            return;
        }
        
        // 根据行为类型计算分数
        double scoreIncrement = calculateActionScore(action);
        
        // 更新各维度偏好
        updatePreferenceScore(userId, "CATEGORY", track.getCategory(), scoreIncrement);
        updatePreferenceScore(userId, "ARTIST", track.getArtist(), scoreIncrement);
        updatePreferenceScore(userId, "GENRE", track.getGenre(), scoreIncrement);
        updatePreferenceScore(userId, "LANGUAGE", track.getLanguage(), scoreIncrement);
        
        // 更新歌曲统计信息
        if ("PLAY".equals(action)) {
            track.setPlayCount(track.getPlayCount() + 1);
            track.setRecentPlayCount(track.getRecentPlayCount() + 1);
            trackMapper.updateById(track);
        } else if ("LIKE".equals(action)) {
            track.setLikeCount(track.getLikeCount() + 1);
            trackMapper.updateById(track);
        }
    }
    
    @Override
    @Transactional
    public void refreshHotRankings() {
        log.info("开始刷新热门榜单");
        
        // 删除旧榜单
        hotRankingMapper.delete(new LambdaQueryWrapper<>());
        
        // 生成新榜单
        generateRanking("DAILY", null, 50);
        generateRanking("WEEKLY", null, 100);
        generateRanking("MONTHLY", null, 200);
        
        // 分类榜单
        List<String> categories = Arrays.asList("流行", "摇滚", "民谣", "经典");
        for (String category : categories) {
            generateRanking("WEEKLY", category, 30);
        }
        
        log.info("热门榜单刷新完成");
    }
    
    @Override
    @Transactional
    public void refreshRecommendationPlaylists() {
        log.info("开始刷新推荐歌单");
        
        // 生成热门歌单
        generateRecommendationPlaylist("HOT", "本周热门", "本周最受欢迎的歌曲合集", 20);
        
        // 生成新歌歌单
        generateRecommendationPlaylist("NEW", "新歌首发", "最新上线的歌曲推荐", 15);
        
        // 生成经典歌单
        generateRecommendationPlaylist("CLASSIC", "经典金曲", "永不过时的经典作品", 25);
        
        log.info("推荐歌单刷新完成");
    }
    
    // ========== 私有辅助方法 ==========
    
    private List<Track> findTracksByPreference(UserPreference preference, int limit) {
        LambdaQueryWrapper<Track> queryWrapper = new LambdaQueryWrapper<Track>()
            .eq(Track::getStatus, "ACTIVE")
            .orderByDesc(Track::getHotScore)
            .last("LIMIT " + limit);
        
        switch (preference.getPreferenceType()) {
            case "CATEGORY":
                queryWrapper.eq(Track::getCategory, preference.getPreferenceValue());
                break;
            case "ARTIST":
                queryWrapper.eq(Track::getArtist, preference.getPreferenceValue());
                break;
            case "GENRE":
                queryWrapper.eq(Track::getGenre, preference.getPreferenceValue());
                break;
            case "LANGUAGE":
                queryWrapper.eq(Track::getLanguage, preference.getPreferenceValue());
                break;
        }
        
        return trackMapper.selectList(queryWrapper);
    }
    
    private double calculateRecommendScore(Track track, List<UserPreference> preferences) {
        double score = 0.0;
        for (UserPreference preference : preferences) {
            if (matchesPreference(track, preference)) {
                score += preference.getScore();
            }
        }
        return Math.min(score / preferences.size(), 1.0);
    }
    
    private boolean matchesPreference(Track track, UserPreference preference) {
        switch (preference.getPreferenceType()) {
            case "CATEGORY":
                return preference.getPreferenceValue().equals(track.getCategory());
            case "ARTIST":
                return preference.getPreferenceValue().equals(track.getArtist());
            case "GENRE":
                return preference.getPreferenceValue().equals(track.getGenre());
            case "LANGUAGE":
                return preference.getPreferenceValue().equals(track.getLanguage());
            default:
                return false;
        }
    }
    
    private String buildSimilarReason(Track baseTrack, Track similarTrack) {
        if (baseTrack.getArtist().equals(similarTrack.getArtist())) {
            return "同一歌手的作品";
        } else if (baseTrack.getCategory().equals(similarTrack.getCategory())) {
            return "相同风格的歌曲";
        } else if (baseTrack.getGenre().equals(similarTrack.getGenre())) {
            return "相似的音乐类型";
        } else {
            return "您可能喜欢的歌曲";
        }
    }
    
    private double calculateSimilarityScore(Track baseTrack, Track similarTrack) {
        double score = 0.0;
        if (baseTrack.getArtist().equals(similarTrack.getArtist())) score += 0.4;
        if (baseTrack.getCategory().equals(similarTrack.getCategory())) score += 0.3;
        if (baseTrack.getGenre().equals(similarTrack.getGenre())) score += 0.2;
        if (baseTrack.getLanguage().equals(similarTrack.getLanguage())) score += 0.1;
        return score;
    }
    
    private TrackRecommendationDto convertToRecommendationDto(Track track, String reason, Double score) {
        TrackRecommendationDto.TrackBasicInfo basicInfo = TrackRecommendationDto.TrackBasicInfo.builder()
            .id(track.getId())
            .title(track.getTitle())
            .artist(track.getArtist())
            .album(track.getAlbum())
            .category(track.getCategory())
            .language(track.getLanguage())
            .genre(track.getGenre())
            .coverUrl(track.getCoverUrl())
            .duration(track.getDuration())
            .hotScore(track.getHotScore())
            .playCount(track.getPlayCount())
            .likeCount(track.getLikeCount())
            .build();
        
        return TrackRecommendationDto.builder()
            .track(basicInfo)
            .recommendReason(reason)
            .recommendScore(score)
            .isHot(track.getIsHot())
            .isNew(track.getIsNew())
            .similarTrackIds(Collections.emptyList()) // 可以后续实现
            .build();
    }
    
    private PlaylistRecommendationDto convertToPlaylistDto(RecommendationPlaylist playlist) {
        // 获取歌单中的前3首歌曲作为预览
        List<RecommendationPlaylistItem> previewItems = recommendationPlaylistItemMapper.selectList(
            new LambdaQueryWrapper<RecommendationPlaylistItem>()
                .eq(RecommendationPlaylistItem::getPlaylistId, playlist.getId())
                .orderByAsc(RecommendationPlaylistItem::getPosition)
                .last("LIMIT 3")
        );
        
        List<TrackRecommendationDto.TrackBasicInfo> previewTracks = new ArrayList<>();
        if (!previewItems.isEmpty()) {
            List<Long> trackIds = previewItems.stream()
                .map(RecommendationPlaylistItem::getTrackId)
                .collect(Collectors.toList());
            
            List<Track> tracks = trackMapper.selectBatchIds(trackIds);
            previewTracks = tracks.stream()
                .map(track -> TrackRecommendationDto.TrackBasicInfo.builder()
                    .id(track.getId())
                    .title(track.getTitle())
                    .artist(track.getArtist())
                    .coverUrl(track.getCoverUrl())
                    .duration(track.getDuration())
                    .build())
                .collect(Collectors.toList());
        }
        
        // 获取歌单总歌曲数
        Long trackCount = recommendationPlaylistItemMapper.selectCount(
            new LambdaQueryWrapper<RecommendationPlaylistItem>()
                .eq(RecommendationPlaylistItem::getPlaylistId, playlist.getId())
        );
        
        return PlaylistRecommendationDto.builder()
            .id(playlist.getId())
            .name(playlist.getName())
            .description(playlist.getDescription())
            .type(playlist.getType())
            .coverUrl(playlist.getCoverUrl())
            .playCount(playlist.getPlayCount())
            .trackCount(trackCount.intValue())
            .previewTracks(previewTracks)
            .sortOrder(playlist.getSortOrder())
            .build();
    }
    
    private double calculateActionScore(String action) {
        switch (action) {
            case "PLAY": return 0.1;
            case "LIKE": return 0.5;
            case "COLLECT": return 0.3;
            case "SKIP": return -0.1;
            case "COMPLETE": return 0.2;
            default: return 0.0;
        }
    }
    
    private void updatePreferenceScore(Long userId, String type, String value, double increment) {
        if (value == null || value.isEmpty()) {
            return;
        }
        
        UserPreference preference = userPreferenceMapper.selectOne(
            new LambdaQueryWrapper<UserPreference>()
                .eq(UserPreference::getUserId, userId)
                .eq(UserPreference::getPreferenceType, type)
                .eq(UserPreference::getPreferenceValue, value)
        );
        
        if (preference == null) {
            preference = new UserPreference();
            preference.setUserId(userId);
            preference.setPreferenceType(type);
            preference.setPreferenceValue(value);
            preference.setScore(Math.max(0.0, increment));
            userPreferenceMapper.insert(preference);
        } else {
            double newScore = Math.max(0.0, Math.min(1.0, preference.getScore() + increment));
            preference.setScore(newScore);
            userPreferenceMapper.updateById(preference);
        }
    }
    
    private void generateRanking(String rankingType, String category, int limit) {
        // 根据类型计算时间范围
        LocalDateTime startTime = calculateRankingStartTime(rankingType);
        
        // 查询排名数据（这里简化处理，实际可以更复杂）
        LambdaQueryWrapper<Track> queryWrapper = new LambdaQueryWrapper<Track>()
            .eq(Track::getStatus, "ACTIVE")
            .orderByDesc(Track::getHotScore)
            .orderByDesc(Track::getRecentPlayCount)
            .last("LIMIT " + limit);
        
        if (category != null) {
            queryWrapper.eq(Track::getCategory, category);
        }
        
        List<Track> tracks = trackMapper.selectList(queryWrapper);
        
        // 生成榜单记录
        for (int i = 0; i < tracks.size(); i++) {
            Track track = tracks.get(i);
            HotRanking ranking = new HotRanking();
            ranking.setRankingType(rankingType);
            ranking.setCategory(category);
            ranking.setTrackId(track.getId());
            ranking.setRankPosition(i + 1);
            ranking.setScore((double) (track.getHotScore() + track.getRecentPlayCount()));
            ranking.setGeneratedAt(LocalDateTime.now());
            
            hotRankingMapper.insert(ranking);
        }
    }
    
    private void generateRecommendationPlaylist(String type, String name, String description, int trackCount) {
        // 删除旧歌单
        RecommendationPlaylist existingPlaylist = recommendationPlaylistMapper.selectOne(
            new LambdaQueryWrapper<RecommendationPlaylist>()
                .eq(RecommendationPlaylist::getType, type)
                .eq(RecommendationPlaylist::getName, name)
        );
        
        if (existingPlaylist != null) {
            recommendationPlaylistItemMapper.delete(
                new LambdaQueryWrapper<RecommendationPlaylistItem>()
                    .eq(RecommendationPlaylistItem::getPlaylistId, existingPlaylist.getId())
            );
            recommendationPlaylistMapper.deleteById(existingPlaylist.getId());
        }
        
        // 创建新歌单
        RecommendationPlaylist playlist = new RecommendationPlaylist();
        playlist.setName(name);
        playlist.setDescription(description);
        playlist.setType(type);
        playlist.setTargetAudience("ALL");
        playlist.setIsActive(true);
        playlist.setSortOrder(getSortOrderByType(type));
        recommendationPlaylistMapper.insert(playlist);
        
        // 添加歌曲
        List<Track> tracks = getTracksByType(type, trackCount);
        for (int i = 0; i < tracks.size(); i++) {
            RecommendationPlaylistItem item = new RecommendationPlaylistItem();
            item.setPlaylistId(playlist.getId());
            item.setTrackId(tracks.get(i).getId());
            item.setPosition(i + 1);
            item.setWeight(1.0 - (double) i / tracks.size()); // 按位置计算权重
            recommendationPlaylistItemMapper.insert(item);
        }
    }
    
    private LocalDateTime calculateRankingStartTime(String rankingType) {
        LocalDateTime now = LocalDateTime.now();
        switch (rankingType) {
            case "DAILY": return now.minusDays(1);
            case "WEEKLY": return now.minusWeeks(1);
            case "MONTHLY": return now.minusMonths(1);
            default: return now.minusDays(7);
        }
    }
    
    private String getRankingTypeName(String rankingType) {
        switch (rankingType) {
            case "DAILY": return "日";
            case "WEEKLY": return "周";
            case "MONTHLY": return "月";
            default: return "";
        }
    }
    
    private int getSortOrderByType(String type) {
        switch (type) {
            case "HOT": return 100;
            case "NEW": return 90;
            case "CLASSIC": return 80;
            default: return 50;
        }
    }
    
    private List<Track> getTracksByType(String type, int limit) {
        LambdaQueryWrapper<Track> queryWrapper = new LambdaQueryWrapper<Track>()
            .eq(Track::getStatus, "ACTIVE")
            .last("LIMIT " + limit);
        
        switch (type) {
            case "HOT":
                queryWrapper.orderByDesc(Track::getHotScore)
                    .orderByDesc(Track::getRecentPlayCount);
                break;
            case "NEW":
                queryWrapper.eq(Track::getIsNew, true)
                    .orderByDesc(Track::getCreatedAt);
                break;
            case "CLASSIC":
                queryWrapper.orderByDesc(Track::getPlayCount)
                    .orderByAsc(Track::getCreatedAt);
                break;
        }
        
        return trackMapper.selectList(queryWrapper);
    }
}
