package com.boxai.domain.dto.wechat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomQrResponse {
    private Long roomId;
    private String qrBase64; // data:image/png;base64,...
}


