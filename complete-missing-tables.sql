-- 完整的数据库表创建脚本
-- 按照依赖关系顺序创建表

-- 1. 基础表（无外键依赖）
create table if not exists t_user (
  id bigint primary key,
  openid varchar(64) unique not null,
  nickname varchar(100),
  avatar_url varchar(512),
  gender int default 0,
  language varchar(10),
  city varchar(50),
  province varchar(50),
  country varchar(50),
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

create table if not exists t_room (
  id bigint primary key,
  name varchar(100) not null,
  description text,
  capacity int default 6,
  status varchar(16) default 'AVAILABLE',
  current_guests int default 0,
  qr_code_url varchar(512),
  qr_version int default 1,
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

create table if not exists t_track (
  id bigint primary key,
  title varchar(200) not null,
  artist varchar(100) not null,
  album varchar(100),
  duration int,
  genre varchar(50),
  language varchar(20),
  release_year int,
  cover_url varchar(512),
  audio_url varchar(512),
  video_url varchar(512),
  lyrics text,
  pinyin_title varchar(400),
  pinyin_artist varchar(200),
  play_count bigint default 0,
  like_count bigint default 0,
  is_available boolean default true,
  quality varchar(16) default 'STANDARD',
  file_size bigint,
  bitrate int,
  sample_rate int,
  channels int default 2,
  format varchar(10) default 'MP4',
  local_path varchar(512),
  cloud_path varchar(512),
  sync_version varchar(32),
  last_sync_time timestamptz,
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

create table if not exists t_scene (
  id bigint primary key,
  name varchar(100) not null,
  description text,
  config_data jsonb,
  is_active boolean default true,
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

-- 2. 依赖基础表的表
create table if not exists t_room_member (
  id bigint primary key,
  room_id bigint references t_room(id) on delete cascade,
  user_id bigint references t_user(id) on delete cascade,
  role varchar(16) default 'GUEST',
  joined_at timestamptz default now(),
  unique(room_id, user_id)
);

create table if not exists t_playlist (
  id bigint primary key,
  room_id bigint references t_room(id) on delete cascade,
  name varchar(100) not null,
  description text,
  is_current boolean default false,
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

create table if not exists t_like (
  id bigint primary key,
  user_id bigint references t_user(id) on delete cascade,
  track_id bigint references t_track(id) on delete cascade,
  created_at timestamptz default now(),
  unique(user_id, track_id)
);

create table if not exists t_user_playlist (
  id bigint primary key,
  user_id bigint references t_user(id) on delete cascade,
  name varchar(100) not null,
  description text,
  is_public boolean default false,
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

create table if not exists t_user_playlist_item (
  id bigint primary key,
  playlist_id bigint references t_user_playlist(id) on delete cascade,
  track_id bigint references t_track(id) on delete cascade,
  sort_order int default 0,
  added_at timestamptz default now()
);

create table if not exists t_playback_history (
  id bigint primary key,
  user_id bigint references t_user(id) on delete cascade,
  room_id bigint references t_room(id) on delete cascade,
  track_id bigint references t_track(id) on delete cascade,
  play_duration int default 0,
  completion_rate decimal(5,2) default 0,
  skip_reason varchar(50),
  played_at timestamptz default now()
);

create table if not exists t_search_log (
  id bigint primary key,
  user_id bigint references t_user(id) on delete set null,
  room_id bigint references t_room(id) on delete set null,
  keyword varchar(200) not null,
  result_count int default 0,
  clicked_track_id bigint references t_track(id) on delete set null,
  searched_at timestamptz default now()
);

create table if not exists t_lighting (
  id bigint primary key,
  room_id bigint references t_room(id) on delete cascade,
  scene_id bigint references t_scene(id) on delete set null,
  brightness int default 50,
  color varchar(7) default '#FFFFFF',
  rhythm varchar(16) default 'STATIC',
  is_auto boolean default false,
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

-- 3. 推荐系统相关表
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

create table if not exists t_recommendation_playlist_item (
  id bigint primary key,
  playlist_id bigint references t_recommendation_playlist(id) on delete cascade,
  track_id bigint references t_track(id) on delete cascade,
  sort_order int default 0,
  weight decimal(3,2) default 1.0,
  created_at timestamptz default now()
);

create table if not exists t_user_preference (
  id bigint primary key,
  user_id bigint references t_user(id) on delete cascade,
  preference_type varchar(32) not null,
  preference_value varchar(100) not null,
  weight decimal(3,2) default 1.0,
  last_updated timestamptz default now(),
  created_at timestamptz default now()
);

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

-- 4. 用户画像相关表
create table if not exists t_user_profile (
  id bigint primary key,
  user_id bigint references t_user(id) on delete cascade,
  preferred_genres jsonb,
  preferred_languages jsonb,
  preferred_decades jsonb,
  preferred_artists jsonb,
  activity_level varchar(16) default 'NORMAL',
  avg_session_duration int default 0,
  total_play_time bigint default 0,
  favorite_time_slots jsonb,
  social_activity_score decimal(5,2) default 0,
  music_diversity_score decimal(5,2) default 0,
  last_analysis_time timestamptz,
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

create table if not exists t_user_behavior (
  id bigint primary key,
  user_id bigint references t_user(id) on delete cascade,
  room_id bigint references t_room(id) on delete cascade,
  behavior_type varchar(32) not null,
  behavior_data jsonb,
  session_id varchar(64),
  occurred_at timestamptz default now()
);

-- 5. AI视频生成相关表
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

-- 创建所有索引
create index if not exists idx_user_openid on t_user(openid);
create index if not exists idx_room_status on t_room(status);
create index if not exists idx_room_member_room on t_room_member(room_id);
create index if not exists idx_room_member_user on t_room_member(user_id);
create index if not exists idx_track_title on t_track(title);
create index if not exists idx_track_artist on t_track(artist);
create index if not exists idx_track_genre on t_track(genre);
create index if not exists idx_track_language on t_track(language);
create index if not exists idx_track_pinyin_title on t_track(pinyin_title);
create index if not exists idx_track_pinyin_artist on t_track(pinyin_artist);
create index if not exists idx_track_sync_version on t_track(sync_version);
create index if not exists idx_playlist_room on t_playlist(room_id);
create index if not exists idx_like_user on t_like(user_id);
create index if not exists idx_like_track on t_like(track_id);
create index if not exists idx_user_playlist_user on t_user_playlist(user_id);
create index if not exists idx_user_playlist_item_playlist on t_user_playlist_item(playlist_id);
create index if not exists idx_playback_history_user on t_playback_history(user_id);
create index if not exists idx_playback_history_room on t_playback_history(room_id);
create index if not exists idx_playback_history_track on t_playback_history(track_id);
create index if not exists idx_playback_history_played_at on t_playback_history(played_at desc);
create index if not exists idx_search_log_user on t_search_log(user_id);
create index if not exists idx_search_log_room on t_search_log(room_id);
create index if not exists idx_search_log_keyword on t_search_log(keyword);
create index if not exists idx_search_log_searched_at on t_search_log(searched_at desc);
create index if not exists idx_lighting_room on t_lighting(room_id);
create index if not exists idx_lighting_scene on t_lighting(scene_id);
create index if not exists idx_recommendation_playlist_type on t_recommendation_playlist(type);
create index if not exists idx_recommendation_playlist_item_playlist on t_recommendation_playlist_item(playlist_id);
create index if not exists idx_user_preference_user on t_user_preference(user_id);
create index if not exists idx_user_preference_type on t_user_preference(preference_type);
create index if not exists idx_media_sync_log_room on t_media_sync_log(room_id);
create index if not exists idx_media_sync_log_track on t_media_sync_log(track_id);
create index if not exists idx_media_sync_log_status on t_media_sync_log(sync_status);
create index if not exists idx_hot_ranking_type on t_hot_ranking(ranking_type);
create index if not exists idx_hot_ranking_track on t_hot_ranking(track_id);
create index if not exists idx_user_profile_user on t_user_profile(user_id);
create index if not exists idx_user_behavior_user on t_user_behavior(user_id);
create index if not exists idx_user_behavior_room on t_user_behavior(room_id);
create index if not exists idx_user_behavior_type on t_user_behavior(behavior_type);
create index if not exists idx_user_behavior_occurred_at on t_user_behavior(occurred_at desc);
create index if not exists idx_ai_video_session_room on t_ai_video_session(room_id);
create index if not exists idx_ai_video_session_stream on t_ai_video_session(stream_id);
create index if not exists idx_ai_video_session_status on t_ai_video_session(status);
create index if not exists idx_ai_video_session_start_time on t_ai_video_session(start_time desc);

-- 添加表注释
comment on table t_user is '用户表';
comment on table t_room is '包间表';
comment on table t_room_member is '包间成员表';
comment on table t_track is '歌曲表';
comment on table t_playlist is '播放列表表';
comment on table t_like is '点赞记录表';
comment on table t_user_playlist is '用户播放列表表';
comment on table t_user_playlist_item is '用户播放列表项表';
comment on table t_playback_history is '播放历史表';
comment on table t_search_log is '搜索日志表';
comment on table t_scene is '场景表';
comment on table t_lighting is '灯光控制表';
comment on table t_recommendation_playlist is '推荐播放列表表';
comment on table t_recommendation_playlist_item is '推荐播放列表项表';
comment on table t_user_preference is '用户偏好表';
comment on table t_media_sync_log is '媒体同步日志表';
comment on table t_hot_ranking is '热门排行表';
comment on table t_user_profile is '用户画像表';
comment on table t_user_behavior is '用户行为表';
comment on table t_ai_video_session is 'AI视频生成会话表';

-- 添加重要列注释（只列出关键字段）
comment on column t_ai_video_session.id is '主键ID（应用生成）';
comment on column t_ai_video_session.room_id is '包间ID';
comment on column t_ai_video_session.user_id is '用户ID';
comment on column t_ai_video_session.stream_id is '流任务ID（唯一）';
comment on column t_ai_video_session.video_type is '视频类型';
comment on column t_ai_video_session.status is '生成状态';
comment on column t_ai_video_session.progress is '生成进度';
comment on column t_ai_video_session.stream_url is '流媒体URL';
comment on column t_ai_video_session.webrtc_url is 'WebRTC流URL';
comment on column t_ai_video_session.hls_url is 'HLS播放URL';
