package com._team._team.memberchat.controller;

import com._team._team.memberchat.config.ChatStompHandler.AuthPrincipal;
import com._team._team.memberchat.dto.req.ChatSendRequest;
import com._team._team.memberchat.error.ChatErrorCode;
import com._team._team.memberchat.error.ChatException;
import com._team._team.memberchat.service.ChatMessageService;
import com._team._team.memberchat.service.ReadReceiptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

// 실시간 메시지·읽음
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final ChatMessageService chatMessageService;
    private final ReadReceiptService readReceiptService;

    // STOMP 연결의 Principal에서 사용자 정보 추출
    private static AuthPrincipal requireAuthPrincipal(Principal principal) {
        if (principal instanceof AuthPrincipal ap) {
            return ap;
        }
        if (principal instanceof Authentication auth && auth.getPrincipal() instanceof AuthPrincipal ap) {
            return ap;
        }
        throw new ChatException(ChatErrorCode.INVALID_JWT, "STOMP 인증 Principal 을 해석할 수 없습니다.");
    }

    // 메시지 전송
    @MessageMapping("/room/{roomId}/send")
    public void send(@DestinationVariable Long roomId,
                     @Payload @Valid ChatSendRequest req,
                     Principal principal) {
        AuthPrincipal p = requireAuthPrincipal(principal);
        chatMessageService.saveAndPublish(p.userId(), roomId, req);
    }

    // 메시지 수정
    @MessageMapping("/message/{messageId}/edit")
    public void edit(@DestinationVariable Long messageId,
                     @Payload Map<String, String> payload,
                     Principal principal) {
        AuthPrincipal p = requireAuthPrincipal(principal);
        chatMessageService.editMessage(p.userId(), messageId, payload.getOrDefault("content", ""));
    }

    // 메시지 삭제
    @MessageMapping("/message/{messageId}/delete")
    public void delete(@DestinationVariable Long messageId,
                       Principal principal) {
        AuthPrincipal p = requireAuthPrincipal(principal);
        chatMessageService.deleteMessage(p.userId(), messageId, false);
    }

    // 읽음 처리
    @MessageMapping("/room/{roomId}/read")
    public void read(@DestinationVariable Long roomId,
                     @Payload Map<String, Object> payload,
                     Principal principal) {
        AuthPrincipal p = requireAuthPrincipal(principal);
        Object rawMsgId = payload.get("messageId");
        if (rawMsgId == null) {
            throw new ChatException(ChatErrorCode.VALIDATION_ERROR, "messageId is required");
        }
        long messageId = Long.parseLong(rawMsgId.toString());
        String deviceId = String.valueOf(payload.getOrDefault("deviceId", ""));
        readReceiptService.ack(p.userId(), roomId, messageId, deviceId);
    }
}
