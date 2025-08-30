-- AI视频生成会话表
create table if not exists t_ai_video_session (
  id bigint primary key,
  room_id bigint references t_room(id) on delete cascade,
  user_id bigint references t_user(id) on delete set null,
  stream_id varchar(64) unique not null,
  video_type varchar(32) not null,
  prompt text,
  style varchar(32),
  resolution varchar(16) default '1920x1080',
  frame_rate int default 30,
  duration int default 60,
  audio_sync boolean default false,
  current_track_id bigint references t_track(id) on delete set null,
  status varchar(16) default 'PENDING',
  progress int default 0,
  stream_url varchar(512),
  webrtc_url varchar(512),
  hls_url varchar(512),
  bitrate int,
  start_time timestamptz default now(),
  end_time timestamptz,
  error_message text,
  generation_info text,
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

-- AI视频会话索引
create index if not exists idx_ai_video_session_room on t_ai_video_session(room_id);
create index if not exists idx_ai_video_session_stream on t_ai_video_session(stream_id);
create index if not exists idx_ai_video_session_status on t_ai_video_session(status);
create index if not exists idx_ai_video_session_start_time on t_ai_video_session(start_time desc);

-- AI视频会话表注释
comment on table t_ai_video_session is 'AI视频生成会话表';
comment on column t_ai_video_session.id is '主键ID（应用生成）';
comment on column t_ai_video_session.room_id is '包间ID';
comment on column t_ai_video_session.user_id is '用户ID';
comment on column t_ai_video_session.stream_id is '流任务ID（唯一）';
comment on column t_ai_video_session.video_type is '视频类型';
comment on column t_ai_video_session.prompt is 'AI提示词';
comment on column t_ai_video_session.style is '视频风格';
comment on column t_ai_video_session.resolution is '视频分辨率';
comment on column t_ai_video_session.frame_rate is '帧率';
comment on column t_ai_video_session.duration is '视频时长（秒）';
comment on column t_ai_video_session.audio_sync is '音频同步';
comment on column t_ai_video_session.current_track_id is '当前播放的歌曲ID';
comment on column t_ai_video_session.status is '生成状态';
comment on column t_ai_video_session.progress is '生成进度';
comment on column t_ai_video_session.stream_url is '流媒体URL';
comment on column t_ai_video_session.webrtc_url is 'WebRTC流URL';
comment on column t_ai_video_session.hls_url is 'HLS播放URL';
comment on column t_ai_video_session.bitrate is '比特率';
comment on column t_ai_video_session.start_time is '开始时间';
comment on column t_ai_video_session.end_time is '结束时间';
comment on column t_ai_video_session.error_message is '错误信息';
comment on column t_ai_video_session.generation_info is '生成参数信息';
comment on column t_ai_video_session.created_at is '创建时间';
comment on column t_ai_video_session.updated_at is '更新时间';

-- 检查是否还缺少其他混合音乐库相关的表
-- 推荐播放列表表
create table if not exists t_recommendation_playlist (
  id bigint primary key,
  name varchar(100) not null,
  type varchar(32) not null,
  description text,
  target_audience varchar(64),
  priority int default 0,
  is_active boolean default true,
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

-- 推荐播放列表项表
create table if not exists t_recommendation_playlist_item (
  id bigint primary key,
  playlist_id bigint references t_recommendation_playlist(id) on delete cascade,
  track_id bigint references t_track(id) on delete cascade,
  sort_order int default 0,
  weight decimal(3,2) default 1.0,
  created_at timestamptz default now()
);

-- 用户偏好表
create table if not exists t_user_preference (
  id bigint primary key,
  user_id bigint references t_user(id) on delete cascade,
  preference_type varchar(32) not null,
  preference_value varchar(100) not null,
  weight decimal(3,2) default 1.0,
  last_updated timestamptz default now(),
  created_at timestamptz default now()
);

-- 热门排行表
create table if not exists t_hot_ranking (
  id bigint primary key,
  track_id bigint references t_track(id) on delete cascade,
  ranking_type varchar(32) not null,
  rank_position int not null,
  score decimal(10,2) default 0,
  play_count bigint default 0,
  like_count bigint default 0,
  period_start date not null,
  period_end date not null,
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

-- 媒体同步日志表
create table if not exists t_media_sync_log (
  id bigint primary key,
  room_id bigint references t_room(id) on delete cascade,
  track_id bigint references t_track(id) on delete cascade,
  sync_type varchar(32) not null,
  sync_status varchar(16) default 'PENDING',
  file_path varchar(512),
  file_size bigint,
  sync_version varchar(32),
  error_message text,
  sync_start_time timestamptz,
  sync_end_time timestamptz,
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

-- 创建索引
create index if not exists idx_recommendation_playlist_type on t_recommendation_playlist(type);
create index if not exists idx_recommendation_playlist_item_playlist on t_recommendation_playlist_item(playlist_id);
create index if not exists idx_user_preference_user on t_user_preference(user_id);
create index if not exists idx_user_preference_type on t_user_preference(preference_type);
create index if not exists idx_hot_ranking_type on t_hot_ranking(ranking_type);
create index if not exists idx_hot_ranking_track on t_hot_ranking(track_id);
create index if not exists idx_media_sync_log_room on t_media_sync_log(room_id);
create index if not exists idx_media_sync_log_track on t_media_sync_log(track_id);
create index if not exists idx_media_sync_log_status on t_media_sync_log(sync_status);

-- 添加表注释
comment on table t_recommendation_playlist is '推荐播放列表表';
comment on table t_recommendation_playlist_item is '推荐播放列表项表';
comment on table t_user_preference is '用户偏好表';
comment on table t_hot_ranking is '热门排行表';
comment on table t_media_sync_log is '媒体同步日志表';

-- 添加列注释
comment on column t_recommendation_playlist.id is '主键ID';
comment on column t_recommendation_playlist.name is '播放列表名称';
comment on column t_recommendation_playlist.type is '推荐类型';
comment on column t_recommendation_playlist.description is '描述';
comment on column t_recommendation_playlist.target_audience is '目标受众';
comment on column t_recommendation_playlist.priority is '优先级';
comment on column t_recommendation_playlist.is_active is '是否激活';

comment on column t_recommendation_playlist_item.id is '主键ID';
comment on column t_recommendation_playlist_item.playlist_id is '播放列表ID';
comment on column t_recommendation_playlist_item.track_id is '歌曲ID';
comment on column t_recommendation_playlist_item.sort_order is '排序';
comment on column t_recommendation_playlist_item.weight is '权重';

comment on column t_user_preference.id is '主键ID';
comment on column t_user_preference.user_id is '用户ID';
comment on column t_user_preference.preference_type is '偏好类型';
comment on column t_user_preference.preference_value is '偏好值';
comment on column t_user_preference.weight is '权重';

comment on column t_hot_ranking.id is '主键ID';
comment on column t_hot_ranking.track_id is '歌曲ID';
comment on column t_hot_ranking.ranking_type is '排行类型';
comment on column t_hot_ranking.rank_position is '排名位置';
comment on column t_hot_ranking.score is '评分';
comment on column t_hot_ranking.play_count is '播放次数';
comment on column t_hot_ranking.like_count is '点赞次数';

comment on column t_media_sync_log.id is '主键ID';
comment on column t_media_sync_log.room_id is '包间ID';
comment on column t_media_sync_log.track_id is '歌曲ID';
comment on column t_media_sync_log.sync_type is '同步类型';
comment on column t_media_sync_log.sync_status is '同步状态';
comment on column t_media_sync_log.file_path is '文件路径';
comment on column t_media_sync_log.file_size is '文件大小';
comment on column t_media_sync_log.sync_version is '同步版本';
comment on column t_media_sync_log.error_message is '错误信息';
comment on column t_media_sync_log.sync_start_time is '同步开始时间';
comment on column t_media_sync_log.sync_end_time is '同步结束时间';
