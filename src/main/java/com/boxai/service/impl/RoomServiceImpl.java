package com.boxai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boxai.domain.dto.room.RoomResetResponse;
import com.boxai.domain.entity.Room;
import com.boxai.domain.entity.RoomMember;
import com.boxai.domain.entity.Playlist;
import com.boxai.domain.mapper.RoomMapper;
import com.boxai.domain.dto.wechat.RoomQrResponse;
import com.boxai.service.RoomService;
import com.boxai.service.RoomMemberService;
import com.boxai.service.PlaylistService;
import com.boxai.service.QrCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl extends ServiceImpl<RoomMapper, Room> implements RoomService {
    private final RoomMemberService roomMemberService;
    private final PlaylistService playlistService;
    private final QrCodeService qrCodeService;
    
    @Override
    @Transactional
    public RoomResetResponse resetRoom(Long roomId) {
        // 获取房间信息
        Room room = getById(roomId);
        if (room == null) {
            throw new RuntimeException("房间不存在");
        }
        
        // 清空房间成员（保留房主）
        roomMemberService.remove(new LambdaQueryWrapper<RoomMember>()
                .eq(RoomMember::getRoomId, roomId)
                .ne(RoomMember::getUserId, room.getOwnerUserId()));
        
        // 清空播放列表
        playlistService.remove(new LambdaQueryWrapper<Playlist>()
                .eq(Playlist::getRoomId, roomId));
        
        // 更新房间状态为重置
        room.setStatus("RESET");
        updateById(room);
        
        // 刷新二维码版本并生成新的二维码
        Integer newVersion = refreshQrVersion(roomId);
        RoomQrResponse newQr = qrCodeService.generateRoomQrWithVersion(roomId, newVersion, null);
        
        // 更新房间状态为活跃
        room.setStatus("ACTIVE");
        updateById(room);
        
        RoomResetResponse response = new RoomResetResponse();
        response.setRoom(room);
        response.setNewQr(newQr);
        response.setMessage("房间已重置，请使用新的二维码邀请客人进入");
        
        return response;
    }
    
    @Override
    public Integer refreshQrVersion(Long roomId) {
        Room room = getById(roomId);
        if (room == null) {
            throw new RuntimeException("房间不存在");
        }
        
        // 版本号+1
        Integer newVersion = (room.getQrVersion() != null ? room.getQrVersion() : 0) + 1;
        room.setQrVersion(newVersion);
        updateById(room);
        
        return newVersion;
    }
}


