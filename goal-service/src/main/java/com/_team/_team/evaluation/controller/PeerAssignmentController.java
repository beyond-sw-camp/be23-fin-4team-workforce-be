package com._team._team.evaluation.controller;

import com._team._team.annotation.Action;
import com._team._team.annotation.CheckPermission;
import com._team._team.annotation.Resource;
import com._team._team.dto.ApiResponse;
import com._team._team.evaluation.dto.reqdto.PeerAssignmentCreateReqDto;
import com._team._team.evaluation.dto.resdto.PeerAssignmentResDto;
import com._team._team.evaluation.service.PeerAssignmentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// 동료 평가 배정, 조회, 완료, 거절 컨트롤러
@RestController
@RequestMapping("/evaluation/peer-assignment")
public class PeerAssignmentController {

    final PeerAssignmentService peerAssignmentService;

    @Autowired
    public PeerAssignmentController(PeerAssignmentService peerAssignmentService) {
        this.peerAssignmentService = peerAssignmentService;
    }

    // 동료 평가 배정
    @CheckPermission(resource = Resource.EVALUATION, action = Action.CREATE)
    @PostMapping
    public ResponseEntity<ApiResponse<PeerAssignmentResDto>> assign(
            @RequestHeader("X-Member-Id") String memberId,
            @Valid @RequestBody PeerAssignmentCreateReqDto dto) {
        PeerAssignmentResDto result = peerAssignmentService.assign(dto, UUID.fromString(memberId));
        return ResponseEntity.ok(ApiResponse.success(result, "동료 평가 배정이 완료되었습니다."));
    }

    // 피평가자별 동료 평가 배정 목록 조회
    @CheckPermission(resource = Resource.EVALUATION, action = Action.READ)
    @GetMapping
    public ResponseEntity<ApiResponse<List<PeerAssignmentResDto>>> listByEvaluatee(
            @RequestParam UUID evaluationId,
            @RequestParam UUID evaluateeId) {
        List<PeerAssignmentResDto> result = peerAssignmentService.listByEvaluatee(evaluationId, evaluateeId);
        return ResponseEntity.ok(ApiResponse.success(result, "동료 평가 배정 목록 조회 성공"));
    }

    // 동료 평가 완료 처리
    @CheckPermission(resource = Resource.EVALUATION, action = Action.UPDATE)
    @PatchMapping("/{peerAssignmentId}/complete")
    public ResponseEntity<ApiResponse<Void>> complete(
            @RequestHeader("X-Member-Id") String memberId,
            @PathVariable UUID peerAssignmentId) {
        peerAssignmentService.complete(peerAssignmentId, UUID.fromString(memberId));
        return ResponseEntity.ok(ApiResponse.success(null, "동료 평가가 완료되었습니다."));
    }

    // 동료 평가 거절 처리
    @CheckPermission(resource = Resource.EVALUATION, action = Action.UPDATE)
    @PatchMapping("/{peerAssignmentId}/decline")
    public ResponseEntity<ApiResponse<Void>> decline(
            @RequestHeader("X-Member-Id") String memberId,
            @PathVariable UUID peerAssignmentId) {
        peerAssignmentService.decline(peerAssignmentId, UUID.fromString(memberId));
        return ResponseEntity.ok(ApiResponse.success(null, "동료 평가가 거절되었습니다."));
    }
}
