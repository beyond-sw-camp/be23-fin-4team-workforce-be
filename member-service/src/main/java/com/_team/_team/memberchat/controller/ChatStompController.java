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

/**
 * 주의: payload 의 sender 정보는 무시한다. 발신자는 Principal 에서 확정된다.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final ChatMessageService chatMessageService;
    private final ReadReceiptService readReceiptService;

    /**
     * STOMP 에서 주입되는 Principal 은 보통 {@link Authentication} 구현체이며,
     * {@link AuthPrincipal} 로 직접 캐스팅하면 ClassCastException 이 난다.
     */
    private static AuthPrincipal requireAuthPrincipal(Principal principal) {
        if (principal instanceof AuthPrincipal ap) {
            return ap;
        }
        if (principal instanceof Authentication auth && auth.getPrincipal() instanceof AuthPrincipal ap) {
            return ap;
        }
        throw new ChatException(ChatErrorCode.INVALID_JWT, "STOMP 인증 Principal 을 해석할 수 없습니다.");
    }

    @MessageMapping("/room/{roomId}/send")
    public void send(@DestinationVariable Long roomId,
                     @Payload @Valid ChatSendRequest req,
                     Principal principal) {
        AuthPrincipal p = requireAuthPrincipal(principal);
        chatMessageService.saveAndPublish(p.userId(), roomId, req);
    }

    @MessageMapping("/message/{messageId}/edit")
    public void edit(@DestinationVariable Long messageId,
                     @Payload Map<String, String> payload,
                     Principal principal) {
        AuthPrincipal p = requireAuthPrincipal(principal);
        chatMessageService.editMessage(p.userId(), messageId, payload.getOrDefault("content", ""));
    }

    @MessageMapping("/message/{messageId}/delete")
    public void delete(@DestinationVariable Long messageId,
                       Principal principal) {
        AuthPrincipal p = requireAuthPrincipal(principal);
        chatMessageService.deleteMessage(p.userId(), messageId, false);
    }

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
