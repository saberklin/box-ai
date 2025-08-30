package com.boxai.domain.dto.device;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * AI视频流控制命令DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiVideoStreamCommand {
    
    private Long roomId;
    private String streamId;
    private String action; // START_STREAM, STOP_STREAM, PAUSE_STREAM, RESUME_STREAM
    private String streamUrl; // RTMP流地址
    private String hlsUrl; // HLS播放地址
    private String webrtcUrl; // WebRTC流地址
    private String resolution; // 分辨率
    private Integer frameRate; // 帧率
    private Boolean audioSync; // 是否音频同步
    private Long trackId; // 关联的歌曲ID
    private Long timestamp;
}
