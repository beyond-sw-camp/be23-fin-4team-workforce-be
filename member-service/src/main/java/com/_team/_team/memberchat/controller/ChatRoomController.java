package com._team._team.memberchat.controller;

import com._team._team.dto.ApiResponse;
import com._team._team.memberchat.dto.req.CreateDirectRoomRequest;
import com._team._team.memberchat.dto.req.CreateGroupRoomRequest;
import com._team._team.memberchat.dto.res.ChatRoomResponse;
import com._team._team.memberchat.service.ChatRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/member-chat/rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @PostMapping("/direct")
    public ResponseEntity<?> createDirect(@RequestHeader("X-User-UUID") UUID me,
                                          @RequestHeader("X-User-CompanyId") UUID companyId,
                                          @RequestBody @Valid CreateDirectRoomRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                chatRoomService.getOrCreateDirectRoom(me, companyId, req), "ok"));
    }

    @PostMapping("/group")
    public ResponseEntity<?> createGroup(@RequestHeader("X-User-UUID") UUID me,
                                         @RequestHeader("X-User-CompanyId") UUID companyId,
                                         @RequestBody @Valid CreateGroupRoomRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                chatRoomService.createGroup(me, companyId, req), "created"));
    }

    @GetMapping
    public ResponseEntity<?> myRooms(@RequestHeader("X-User-UUID") UUID me) {
        List<ChatRoomResponse> rooms = chatRoomService.myRooms(me);
        return ResponseEntity.ok(ApiResponse.success(rooms, "ok"));
    }

    @PostMapping("/{roomId}/members/{memberId}")
    public ResponseEntity<?> addMember(@RequestHeader("X-User-UUID") UUID me,
                                       @PathVariable Long roomId,
                                       @PathVariable UUID memberId) {
        chatRoomService.addMember(me, roomId, memberId);
        return ResponseEntity.ok(ApiResponse.success(null, "added"));
    }

    @PostMapping("/{roomId}/leave")
    public ResponseEntity<?> leave(@RequestHeader("X-User-UUID") UUID me,
                                   @PathVariable Long roomId) {
        chatRoomService.leave(me, roomId);
        return ResponseEntity.ok(ApiResponse.success(null, "left"));
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<?> delete(@RequestHeader("X-User-UUID") UUID me,
                                    @RequestHeader(value = "X-User-Role", required = false) String role,
                                    @PathVariable Long roomId) {
        boolean adminOverride = "HR_ADMIN".equals(role);
        chatRoomService.deleteRoom(me, roomId, adminOverride);
        return ResponseEntity.ok(ApiResponse.success(null, "deleted"));
    }
}
