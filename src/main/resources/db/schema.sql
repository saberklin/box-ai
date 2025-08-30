-- Box-AI Karaoke 数据库结构（PostgreSQL）
-- 说明：ID 使用应用侧生成（MyBatis-Plus assign_id），故表中为 bigint 主键，无序列默认值。

-- 用户
create table if not exists t_user (
  id bigint primary key,
  open_id varchar(64) unique,
  union_id varchar(64),
  nickname varchar(64),
  avatar_url varchar(255),
  phone varchar(32),
  gender smallint, -- 0未知 1男 2女
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

-- 包厢/房间
create table if not exists t_room (
  id bigint primary key,
  room_code varchar(64) unique,
  name varchar(64),
  owner_user_id bigint references t_user(id),
  status varchar(16) default 'ACTIVE', -- ACTIVE/RESET/CLOSED
  qr_version int default 1, -- 二维码版本号，用于控制二维码有效性
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

-- 房间成员与权限
create table if not exists t_room_member (
  id bigint primary key,
  room_id bigint references t_room(id) on delete cascade,
  user_id bigint references t_user(id) on delete cascade,
  role varchar(16) not null default 'NORMAL', -- OWNER/NORMAL
  created_at timestamptz default now(),
  updated_at timestamptz default now(),
  unique(room_id, user_id)
);

-- 曲库（增强版 - 支持混合架构）
create table if not exists t_track (
  id bigint primary key,
  title varchar(128) not null,
  artist varchar(128) not null,
  album varchar(128),
  category varchar(64),
  language varchar(32),
  genre varchar(64),
  tags varchar(256),
  
  -- 媒体文件信息（本地存储）
  local_file_path varchar(512),
  file_size bigint,
  duration int,
  video_quality varchar(16),
  audio_quality varchar(16),
  
  -- 云端元数据
  cover_url varchar(512),
  lyrics_url varchar(512),
  preview_url varchar(512),
  
  -- 推荐算法相关
  hot_score int default 0,
  play_count bigint default 0,
  like_count int default 0,
  recent_play_count int default 0,
  
  -- 状态标识
  is_new boolean default false,
  is_hot boolean default false,
  is_recommended boolean default false,
  status varchar(16) default 'ACTIVE',
  
  -- 版权和同步信息
  copyright_info varchar(256),
  sync_version bigint default 1,
  last_sync_at timestamptz,
  
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

-- 曲库索引优化
create index if not exists idx_track_title on t_track(title);
create index if not exists idx_track_artist on t_track(artist);
create index if not exists idx_track_category on t_track(category);
create index if not exists idx_track_hot_score on t_track(hot_score desc);
create index if not exists idx_track_play_count on t_track(play_count desc);
create index if not exists idx_track_status on t_track(status);
create index if not exists idx_track_sync_version on t_track(sync_version);

-- 播放队列
create table if not exists t_playlist (
  id bigint primary key,
  room_id bigint references t_room(id) on delete cascade,
  track_id bigint references t_track(id),
  ordered_by_user_id bigint references t_user(id),
  position int not null,
  status varchar(16) not null default 'QUEUED', -- QUEUED/PLAYING/DONE
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);
create index if not exists idx_playlist_room_pos on t_playlist(room_id, position);

-- 点赞/收藏
create table if not exists t_like (
  id bigint primary key,
  user_id bigint references t_user(id) on delete cascade,
  track_id bigint references t_track(id) on delete cascade,
  created_at timestamptz default now(),
  updated_at timestamptz default now(),
  unique(user_id, track_id)
);

-- 用户自建歌单
create table if not exists t_user_playlist (
  id bigint primary key,
  user_id bigint references t_user(id) on delete cascade,
  name varchar(64) not null,
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);
create table if not exists t_user_playlist_item (
  id bigint primary key,
  playlist_id bigint references t_user_playlist(id) on delete cascade,
  track_id bigint references t_track(id),
  position int,
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

-- 播放历史
create table if not exists t_playback_history (
  id bigint primary key,
  room_id bigint references t_room(id) on delete set null,
  track_id bigint references t_track(id),
  user_id bigint references t_user(id),
  started_at timestamptz default now(),
  ended_at timestamptz,
  rating smallint,
  is_replay boolean default false
);

-- 搜索关键词日志（用于热搜/推荐）
create table if not exists t_search_log (
  id bigint primary key,
  room_id bigint references t_room(id) on delete set null,
  user_id bigint references t_user(id) on delete set null,
  keyword varchar(128) not null,
  created_at timestamptz default now()
);
create index if not exists idx_search_log_keyword on t_search_log(keyword);

-- 推荐歌单（系统生成的推荐列表）
create table if not exists t_recommendation_playlist (
  id bigint primary key,
  name varchar(128) not null,
  description varchar(256),
  type varchar(32) not null,
  target_audience varchar(64),
  cover_url varchar(512),
  play_count bigint default 0,
  is_active boolean default true,
  sort_order int default 0,
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

-- 推荐歌单曲目
create table if not exists t_recommendation_playlist_item (
  id bigint primary key,
  playlist_id bigint references t_recommendation_playlist(id) on delete cascade,
  track_id bigint references t_track(id) on delete cascade,
  position int not null,
  weight double precision default 1.0,
  created_at timestamptz default now(),
  unique(playlist_id, track_id)
);

-- 用户偏好标签（基于行为分析）
create table if not exists t_user_preference (
  id bigint primary key,
  user_id bigint references t_user(id) on delete cascade,
  preference_type varchar(32) not null,
  preference_value varchar(128) not null,
  score double precision default 1.0,
  updated_at timestamptz default now(),
  unique(user_id, preference_type, preference_value)
);

-- 本地媒体文件同步记录
create table if not exists t_media_sync_log (
  id bigint primary key,
  room_id bigint references t_room(id),
  track_id bigint references t_track(id) on delete cascade,
  sync_type varchar(16) not null,
  file_path varchar(512),
  file_size bigint,
  sync_status varchar(16) default 'PENDING',
  error_message text,
  started_at timestamptz default now(),
  completed_at timestamptz
);

-- 热门榜单缓存（定时计算生成）
create table if not exists t_hot_ranking (
  id bigint primary key,
  ranking_type varchar(32) not null,
  category varchar(64),
  track_id bigint references t_track(id) on delete cascade,
  rank_position int not null,
  score double precision not null,
  generated_at timestamptz default now(),
  unique(ranking_type, category, track_id)
);

-- 索引优化
create index if not exists idx_recommendation_playlist_type on t_recommendation_playlist(type);
create index if not exists idx_recommendation_playlist_active on t_recommendation_playlist(is_active, sort_order);
create index if not exists idx_user_preference_user on t_user_preference(user_id);
create index if not exists idx_media_sync_room on t_media_sync_log(room_id);
create index if not exists idx_media_sync_status on t_media_sync_log(sync_status);
create index if not exists idx_hot_ranking_type on t_hot_ranking(ranking_type, rank_position);

-- AI 场景
create table if not exists t_scene (
  id bigint primary key,
  room_id bigint references t_room(id) on delete cascade,
  type varchar(32), -- 梦幻/科技/自然 等
  state_json text,
  created_at timestamptz default now(),
  updated_at timestamptz default now(),
  unique(room_id)
);

-- 灯光控制
create table if not exists t_lighting (
  id bigint primary key,
  room_id bigint references t_room(id) on delete cascade,
  brightness int check (brightness between 0 and 100),
  color varchar(16), -- #RRGGBB
  rhythm varchar(32),
  created_at timestamptz default now(),
  updated_at timestamptz default now(),
  unique(room_id)
);

-- 索引（便于搜索）
create index if not exists idx_track_title on t_track(lower(title));
create index if not exists idx_track_artist on t_track(lower(artist));
-- 简单全文索引（可选）
create index if not exists idx_track_ft on t_track using gin (to_tsvector('simple', coalesce(title,'') || ' ' || coalesce(artist,'') || ' ' || coalesce(tags,'')));


-- 字段注释
-- t_user
comment on table t_user is '用户表';
comment on column t_user.id is '主键ID（应用生成）';
comment on column t_user.open_id is '微信 openid';
comment on column t_user.union_id is '微信 unionid（同主体多应用统一）';
comment on column t_user.nickname is '昵称';
comment on column t_user.avatar_url is '头像URL';
comment on column t_user.phone is '手机号';
comment on column t_user.gender is '性别：0未知 1男 2女';
comment on column t_user.created_at is '创建时间';
comment on column t_user.updated_at is '更新时间';

-- t_room
comment on table t_room is '包厢/房间表';
comment on column t_room.id is '主键ID（应用生成）';
comment on column t_room.room_code is '房间码/二维码唯一编号';
comment on column t_room.name is '房间名称';
comment on column t_room.owner_user_id is '房主用户ID';
comment on column t_room.status is '房间状态：OPEN/CLOSED';
comment on column t_room.created_at is '创建时间';
comment on column t_room.updated_at is '更新时间';

-- t_room_member
comment on table t_room_member is '房间成员表';
comment on column t_room_member.id is '主键ID（应用生成）';
comment on column t_room_member.room_id is '房间ID';
comment on column t_room_member.user_id is '用户ID';
comment on column t_room_member.role is '成员角色：OWNER/NORMAL';
comment on column t_room_member.created_at is '创建时间';
comment on column t_room_member.updated_at is '更新时间';

-- t_track
comment on table t_track is '曲库表';
comment on column t_track.id is '主键ID（应用生成）';
comment on column t_track.title is '歌曲名称';
comment on column t_track.artist is '歌手名';
comment on column t_track.category is '歌曲分类';
comment on column t_track.tags is '标签（逗号分隔）';
comment on column t_track.media_url is '媒体播放地址';
comment on column t_track.cover_url is '封面图片地址';
comment on column t_track.hot_score is '热度分';
comment on column t_track.is_new is '是否新歌';
comment on column t_track.created_at is '创建时间';
comment on column t_track.updated_at is '更新时间';

-- t_playlist
comment on table t_playlist is '播放队列表';
comment on column t_playlist.id is '主键ID（应用生成）';
comment on column t_playlist.room_id is '房间ID';
comment on column t_playlist.track_id is '歌曲ID';
comment on column t_playlist.ordered_by_user_id is '点歌用户ID';
comment on column t_playlist.position is '队列位置（越小越靠前）';
comment on column t_playlist.status is '状态：QUEUED/PLAYING/DONE';
comment on column t_playlist.created_at is '创建时间';
comment on column t_playlist.updated_at is '更新时间';

-- t_like
comment on table t_like is '点赞/收藏表';
comment on column t_like.id is '主键ID（应用生成）';
comment on column t_like.user_id is '用户ID';
comment on column t_like.track_id is '歌曲ID';
comment on column t_like.created_at is '创建时间';
comment on column t_like.updated_at is '更新时间';

-- t_user_playlist
comment on table t_user_playlist is '用户自建歌单表';
comment on column t_user_playlist.id is '主键ID（应用生成）';
comment on column t_user_playlist.user_id is '用户ID';
comment on column t_user_playlist.name is '歌单名称';
comment on column t_user_playlist.created_at is '创建时间';
comment on column t_user_playlist.updated_at is '更新时间';

-- t_user_playlist_item
comment on table t_user_playlist_item is '用户歌单明细表';
comment on column t_user_playlist_item.id is '主键ID（应用生成）';
comment on column t_user_playlist_item.playlist_id is '歌单ID';
comment on column t_user_playlist_item.track_id is '歌曲ID';
comment on column t_user_playlist_item.position is '在歌单中的顺序';
comment on column t_user_playlist_item.created_at is '创建时间';
comment on column t_user_playlist_item.updated_at is '更新时间';

-- t_playback_history
comment on table t_playback_history is '播放历史表';
comment on column t_playback_history.id is '主键ID（应用生成）';
comment on column t_playback_history.room_id is '房间ID';
comment on column t_playback_history.track_id is '歌曲ID';
comment on column t_playback_history.user_id is '操作用户ID';
comment on column t_playback_history.started_at is '开始播放时间';
comment on column t_playback_history.ended_at is '结束播放时间';
comment on column t_playback_history.rating is '评分（可空）';
comment on column t_playback_history.is_replay is '是否重唱/重播';

-- t_search_log
comment on table t_search_log is '搜索关键词日志';
comment on column t_search_log.id is '主键ID（应用生成）';
comment on column t_search_log.room_id is '房间ID（可空）';
comment on column t_search_log.user_id is '用户ID（可空）';
comment on column t_search_log.keyword is '搜索关键词';
comment on column t_search_log.created_at is '创建时间';

-- t_scene
comment on table t_scene is 'AI 场景状态表';
comment on column t_scene.id is '主键ID（应用生成）';
comment on column t_scene.room_id is '房间ID（唯一）';
comment on column t_scene.type is '场景类型（梦幻/科技/自然等）';
comment on column t_scene.state_json is '场景状态JSON';
comment on column t_scene.created_at is '创建时间';
comment on column t_scene.updated_at is '更新时间';

-- t_lighting
comment on table t_lighting is '灯光控制表';
comment on column t_lighting.id is '主键ID（应用生成）';
comment on column t_lighting.room_id is '房间ID（唯一）';
comment on column t_lighting.brightness is '亮度 0-100';
comment on column t_lighting.color is '颜色 #RRGGBB';
comment on column t_lighting.rhythm is '节奏模式';
comment on column t_lighting.created_at is '创建时间';
comment on column t_lighting.updated_at is '更新时间';

-- 用户画像表
create table if not exists t_user_profile (
  id bigint primary key,
  user_id bigint references t_user(id) on delete cascade,
  favorite_categories jsonb, -- 偏好歌曲类型统计 {"流行": 25, "摇滚": 15, "民谣": 10}
  total_play_count int default 0, -- 总播放次数
  total_like_count int default 0, -- 总点赞次数
  total_search_count int default 0, -- 总搜索次数
  active_days int default 0, -- 活跃天数
  last_active_date date, -- 最后活跃日期
  avg_session_duration int default 0, -- 平均会话时长(分钟)
  preferred_time_slots jsonb, -- 偏好时间段 {"morning": 5, "afternoon": 10, "evening": 20, "night": 8}
  created_at timestamptz default now(),
  updated_at timestamptz default now(),
  unique(user_id)
);

-- 用户行为记录表（用于计算画像）
create table if not exists t_user_behavior (
  id bigint primary key,
  user_id bigint references t_user(id) on delete cascade,
  behavior_type varchar(32) not null, -- PLAY/LIKE/SEARCH/LOGIN
  target_id bigint, -- 关联的目标ID（曲目ID、搜索词等）
  target_type varchar(32), -- TRACK/SEARCH_KEYWORD
  metadata jsonb, -- 额外元数据 {"category": "流行", "duration": 180}
  session_id varchar(64), -- 会话ID
  created_at timestamptz default now()
);

-- 用户画像相关索引
create index if not exists idx_user_profile_user_id on t_user_profile(user_id);
create index if not exists idx_user_profile_last_active on t_user_profile(last_active_date desc);
create index if not exists idx_user_behavior_user_id on t_user_behavior(user_id);
create index if not exists idx_user_behavior_type on t_user_behavior(behavior_type);
create index if not exists idx_user_behavior_created_at on t_user_behavior(created_at desc);
create index if not exists idx_user_behavior_session on t_user_behavior(session_id);

-- 用户画像表注释
comment on table t_user_profile is '用户画像表';
comment on column t_user_profile.id is '主键ID（应用生成）';
comment on column t_user_profile.user_id is '用户ID（唯一）';
comment on column t_user_profile.favorite_categories is '偏好歌曲类型JSON统计';
comment on column t_user_profile.total_play_count is '总播放次数';
comment on column t_user_profile.total_like_count is '总点赞次数';
comment on column t_user_profile.total_search_count is '总搜索次数';
comment on column t_user_profile.active_days is '活跃天数';
comment on column t_user_profile.last_active_date is '最后活跃日期';
comment on column t_user_profile.avg_session_duration is '平均会话时长(分钟)';
comment on column t_user_profile.preferred_time_slots is '偏好时间段JSON';
comment on column t_user_profile.created_at is '创建时间';
comment on column t_user_profile.updated_at is '更新时间';

-- 用户行为记录表注释
comment on table t_user_behavior is '用户行为记录表';
comment on column t_user_behavior.id is '主键ID（应用生成）';
comment on column t_user_behavior.user_id is '用户ID';
comment on column t_user_behavior.behavior_type is '行为类型';
comment on column t_user_behavior.target_id is '目标ID';
comment on column t_user_behavior.target_type is '目标类型';
comment on column t_user_behavior.metadata is '元数据JSON';
comment on column t_user_behavior.session_id is '会话ID';
comment on column t_user_behavior.created_at is '创建时间';

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
