package com.boxai.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boxai.common.web.ApiResponse;
import com.boxai.domain.entity.Room;
import com.boxai.domain.entity.RoomMember;
import com.boxai.domain.dto.wechat.RoomQrResponse;
import com.boxai.domain.dto.room.RoomResetResponse;
import com.boxai.domain.dto.request.RoomBindRequest;
import com.boxai.service.RoomMemberService;
import com.boxai.service.RoomService;
import com.boxai.service.QrCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 房间管理控制器
 * 提供房间绑定、成员管理、二维码生成等功能
 */
@RestController
@RequestMapping("/api/rooms")
@Validated
@RequiredArgsConstructor
@Tag(name = "房间管理", description = "KTV房间管理相关API")
public class RoomController {
    private final RoomService roomService;
    private final RoomMemberService memberService;
    private final QrCodeService qrCodeService;

    /**
     * 绑定房间
     * 根据房间编号绑定或创建房间，并将用户加入房间成员
     * @param req 绑定请求（包含房间编号和用户ID）
     * @return 房间信息
     */
    @PostMapping("/bind")
    @Transactional
    @Operation(summary = "绑定房间", 
               description = "通过房间编号绑定或创建新房间，并将当前用户加入房间成员")

    public ApiResponse<Room> bind(
            @Parameter(description = "房间绑定请求参数", required = true)
            @RequestBody RoomBindRequest req) {
        Room room = roomService.getOne(new LambdaQueryWrapper<Room>().eq(Room::getRoomCode, req.getRoomCode()));
        if (room == null) {
            room = new Room();
            room.setRoomCode(req.getRoomCode());
            room.setName("房间" + req.getRoomCode());
            room.setOwnerUserId(req.getUserId());
            roomService.save(room);
        }

        RoomMember existing = memberService.getOne(new LambdaQueryWrapper<RoomMember>()
                .eq(RoomMember::getRoomId, room.getId())
                .eq(RoomMember::getUserId, req.getUserId()));
        if (existing == null) {
            RoomMember m = new RoomMember();
            m.setRoomId(room.getId());
            m.setUserId(req.getUserId());
            m.setRole(room.getOwnerUserId().equals(req.getUserId()) ? "OWNER" : "NORMAL");
            memberService.save(m);
        }
        return ApiResponse.ok(room);
    }

    /**
     * 获取房间成员列表
     * @param roomId 房间ID
     * @return 成员列表（包含用户ID、角色等信息）
     */
    @GetMapping("/{roomId}/members")
    @Operation(summary = "获取房间成员列表", 
               description = "查询指定房间的所有成员信息")

    public ApiResponse<List<RoomMember>> members(
            @Parameter(description = "房间ID", required = true, example = "1")
            @PathVariable Long roomId) {
        List<RoomMember> list = memberService.list(new LambdaQueryWrapper<RoomMember>().eq(RoomMember::getRoomId, roomId));
        return ApiResponse.ok(list);
    }

    /**
     * 生成房间小程序码
     * 为指定房间生成唯一的微信小程序码，用于快速进入房间
     * @param roomId 房间ID
     * @param page 小程序页面路径（可选，默认为首页）
     * @return 二维码信息（Base64编码的PNG图片）
     */
    @GetMapping("/{roomId}/qr")
    @Operation(summary = "生成房间小程序码", 
               description = "为指定房间生成微信小程序码，用于邀请其他用户扫码进入")

    public ApiResponse<RoomQrResponse> generateQr(
            @Parameter(description = "房间ID", required = true, example = "1")
            @PathVariable Long roomId, 
            @Parameter(description = "小程序页面路径", example = "pages/room/room")
            @RequestParam(required = false) String page) {
        return ApiResponse.ok(qrCodeService.generateRoomQr(roomId, page));
    }

    /**
     * 刷新房间二维码
     * 使当前二维码失效，生成新的二维码（版本号+1）
     * @param roomId 房间ID
     * @param page 小程序页面路径（可选）
     * @return 新的二维码信息
     */
    @PostMapping("/{roomId}/qr/refresh")
    @Operation(summary = "刷新房间二维码", 
               description = "使当前二维码失效，重新生成新版本的二维码")

    public ApiResponse<RoomQrResponse> refreshQr(
            @Parameter(description = "房间ID", required = true, example = "1")
            @PathVariable Long roomId, 
            @Parameter(description = "小程序页面路径", example = "pages/room/room")
            @RequestParam(required = false) String page) {
        Integer newVersion = roomService.refreshQrVersion(roomId);
        return ApiResponse.ok(qrCodeService.generateRoomQrWithVersion(roomId, newVersion, page));
    }

    /**
     * 重置房间
     * 清空房间成员、播放列表，并刷新二维码（适用于客人变更场景）
     * @param roomId 房间ID
     * @return 重置后的房间信息和新二维码
     */
    @PostMapping("/{roomId}/reset")
    @Operation(summary = "重置房间", 
               description = "清空房间成员（保留房主）和播放列表，重新生成二维码")

    @Transactional
    public ApiResponse<RoomResetResponse> resetRoom(
            @Parameter(description = "房间ID", required = true, example = "1")
            @PathVariable Long roomId) {
        return ApiResponse.ok(roomService.resetRoom(roomId));
    }


    

}


