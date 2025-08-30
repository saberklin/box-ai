-- 检查数据库中是否存在AI相关的表
SELECT 
    schemaname,
    tablename,
    tableowner
FROM pg_tables 
WHERE tablename IN (
    't_ai_video_session',
    't_recommendation_playlist',
    't_recommendation_playlist_item', 
    't_user_preference',
    't_hot_ranking',
    't_media_sync_log'
)
ORDER BY tablename;
