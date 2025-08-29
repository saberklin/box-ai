package com.boxai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.boxai.domain.entity.RoomMember;
import com.boxai.domain.mapper.RoomMemberMapper;
import com.boxai.service.RoomMemberService;
import org.springframework.stereotype.Service;

@Service
public class RoomMemberServiceImpl extends ServiceImpl<RoomMemberMapper, RoomMember> implements RoomMemberService {
}


