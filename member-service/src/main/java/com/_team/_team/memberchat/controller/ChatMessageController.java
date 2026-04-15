package com._team._team.memberchat.controller;

import com._team._team.dto.ApiResponse;
import com._team._team.memberchat.dto.req.ChatSendRequest;
import com._team._team.memberchat.dto.res.ChatMessageEvent;
import com._team._team.memberchat.dto.res.ChatMessageResponse;
import com._team._team.memberchat.service.ChatMessageService;
import com._team._team.memberchat.service.ReadReceiptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST 폴백 / 히스토리 / 동기화 엔드포인트.
 * JWT 는 Gateway JwtAuthFilter 가 검증하고 X-User-UUID 헤더로 주입한다고 가정.
 * (member-service 내에서 독립 실행 시엔 별도 필터로 parse 필요)
 */
@RestController
@RequestMapping("/member-chat")
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService chatMessageService;
    private final ReadReceiptService readReceiptService;

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<?> history(@RequestHeader("X-User-UUID") UUID memberId,
                                     @PathVariable Long roomId,
                                     @RequestParam(required = false) Long cursor,
                                     @RequestParam(defaultValue = "50") int size) {
        List<ChatMessageResponse> messages = chatMessageService.history(memberId, roomId, cursor, size);
        return ResponseEntity.ok(ApiResponse.success(messages, "ok"));
    }

    /**
     * 웹 클라이언트용 텍스트 전송 폴백. STOMP 와 동일하게 {@link ChatMessageService#saveAndPublish} 를 호출한다.
     */
    @PostMapping("/rooms/{roomId}/messages")
    public ResponseEntity<?> send(@RequestHeader("X-User-UUID") UUID memberId,
                                  @PathVariable Long roomId,
                                  @RequestBody @Valid ChatSendRequest req) {
        ChatMessageEvent event = chatMessageService.saveAndPublish(memberId, roomId, req);
        return ResponseEntity.ok(ApiResponse.success(event, "sent"));
    }

    @GetMapping("/sync")
    public ResponseEntity<?> sync(@RequestHeader("X-User-UUID") UUID memberId,
                                  @RequestParam Long roomId,
                                  @RequestParam(required = false) Long lastSeenMessageId,
                                  @RequestParam(defaultValue = "200") int size) {
        List<ChatMessageResponse> messages = chatMessageService.sync(memberId, roomId, lastSeenMessageId, size);
        return ResponseEntity.ok(ApiResponse.success(messages, "ok"));
    }

    @PatchMapping("/messages/{messageId}")
    public ResponseEntity<?> edit(@RequestHeader("X-User-UUID") UUID memberId,
                                  @PathVariable Long messageId,
                                  @RequestBody Map<String, String> body) {
        ChatMessageEvent updated = chatMessageService.editMessage(memberId, messageId, body.getOrDefault("content", ""));
        return ResponseEntity.ok(ApiResponse.success(updated, "updated"));
    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<?> delete(@RequestHeader("X-User-UUID") UUID memberId,
                                    @RequestHeader(value = "X-User-Role", required = false) String role,
                                    @PathVariable Long messageId) {
        boolean admin = "HR_ADMIN".equals(role) || "AUDITOR".equals(role);
        ChatMessageEvent event = chatMessageService.deleteMessage(memberId, messageId, admin);
        return ResponseEntity.ok(ApiResponse.success(event, "deleted"));
    }

    @PostMapping("/rooms/{roomId}/read")
    public ResponseEntity<?> read(@RequestHeader("X-User-UUID") UUID memberId,
                                  @PathVariable Long roomId,
                                  @RequestParam Long messageId,
                                  @RequestParam(required = false, defaultValue = "") String deviceId) {
        readReceiptService.ack(memberId, roomId, messageId, deviceId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("ok", true), "ack"));
    }
}
