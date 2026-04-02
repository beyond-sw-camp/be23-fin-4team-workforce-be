package com._team._team.controller;

import com._team._team.dto.ApiResponse;
import com._team._team.dtos.reqdto.ApprovalRequestCreateDto;
import com._team._team.dtos.reqdto.CancelRequestDto;
import com._team._team.service.RequestService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/request")
public class RequestController {

    private final RequestService requestService;

    @Autowired
    public RequestController(RequestService requestService) {
        this.requestService = requestService;
    }

//    결재요청 생성
    @PostMapping("/create")
    public ResponseEntity<?> createRequest(
            @RequestHeader("X-User-UUID") UUID memberId,
            @RequestBody ApprovalRequestCreateDto dto) {
        UUID requestId = requestService.createApprovalRequest(memberId, dto);
        String message = dto.isDraft() ? "임시저장 성공" : "결재 요청 생성 성공";
        return new ResponseEntity<>(
                ApiResponse.success(requestId, "결재 요청 생성 성공"),
                HttpStatus.CREATED
        );
    }

//    임시저장된 결재 제출
    @PostMapping("/{requestId}/submit")
    public ResponseEntity<?> submitDraftRequest(
            @RequestHeader("X-User-UUID") UUID memberId,
            @PathVariable UUID requestId,
            @RequestBody ApprovalRequestCreateDto dto) {
        UUID resultId = requestService.submitDraftRequest(memberId, requestId, dto);
        return new ResponseEntity<>(
                ApiResponse.success(resultId, "임시저장 건 제출 성공"),
                HttpStatus.OK
        );
    }

//    임시저장 결재요청 수정
    @PutMapping("/{requestId}/draft")
    public ResponseEntity<?> updateDraftRequest(
            @RequestHeader("X-User-UUID") UUID memberId,
            @PathVariable UUID requestId,
            @RequestBody ApprovalRequestCreateDto dto) {
        requestService.updateDraftRequest(memberId, requestId, dto);
        return new ResponseEntity<>(
                ApiResponse.success(requestId, "임시저장 수정 성공"),
                HttpStatus.OK
        );
    }

    @GetMapping("/my-list")
    public ResponseEntity<?> getMyRequests(
            @RequestHeader("X-User-UUID") UUID memberId) {
        return new ResponseEntity<>(
                ApiResponse.success(requestService.getMyRequests(memberId), "내 결재 목록 조회 성공"),
                HttpStatus.OK
        );
    }

//    내 임시저장 목록 조회
    @GetMapping("/my-drafts")
    public ResponseEntity<?> getMyDraftRequests(
            @RequestHeader("X-User-UUID") UUID memberId) {
        return new ResponseEntity<>(
                ApiResponse.success(requestService.getMyDraftRequests(memberId), "임시저장 목록 조회 성공"),
                HttpStatus.OK
        );
    }

//    결재 요청 단건 상세 조회
    @GetMapping("/{requestId}")
    public ResponseEntity<?> getRequestDetail(@PathVariable UUID requestId) {
        return new ResponseEntity<>(
                ApiResponse.success(requestService.getRequestDetail(requestId), "결재 요청 상세 조회 성공"),
                HttpStatus.OK
        );
    }

    @PostMapping("/{requestId}/cancel")
    public ResponseEntity<?> cancelRequest(
            @RequestHeader("X-User-UUID") UUID memberId,
            @PathVariable UUID requestId,
            @Valid @RequestBody CancelRequestDto dto) {
        requestService.cancelRequest(memberId, requestId, dto.getCancelReason());
        return new ResponseEntity<>(
                ApiResponse.success(requestId, "결재 요청 회수 성공"),
                HttpStatus.OK
        );
    }

//    임시저장 건 삭제
    @DeleteMapping("/{requestId}/draft")
    public ResponseEntity<?> deleteDraftRequest(
            @RequestHeader("X-User-UUID") UUID memberId,
            @PathVariable UUID requestId) {
        requestService.deleteDraftRequest(memberId, requestId);
        return new ResponseEntity<>(
                ApiResponse.success(requestId, "임시저장 삭제 성공"),
                HttpStatus.OK
        );
    }
}
