package com._team._team.controller;

import com._team._team.dto.ApiResponse;
import com._team._team.dtos.reqdto.ApprovalPolicyLineCreateDto;
import com._team._team.dtos.resdto.ApprovalPolicyLineResponseDto;
import com._team._team.dtos.resdto.ResolvedApprovalLineDto;
import com._team._team.service.ApprovalLineResolverService;
import com._team._team.service.ApprovalPolicyLineService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/approvalPolicyLine")
// url 예시 : http://localhost:8080/member(각서비스이름)/member(리퀘스트메핑)/health(각api)
public class ApprovalPolicyLineController {
    private final ApprovalPolicyLineService policyLineService;
    private final ApprovalLineResolverService resolverService;

    @Autowired
    public ApprovalPolicyLineController(
            ApprovalPolicyLineService policyLineService,
            ApprovalLineResolverService resolverService) {
        this.policyLineService = policyLineService;
        this.resolverService = resolverService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createPolicyLine(@RequestBody @Valid ApprovalPolicyLineCreateDto dto) {
        policyLineService.createPolicyLine(dto);
        return new ResponseEntity<>(
                ApiResponse.success(null, "결재 정책 라인 생성 성공"),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/policyLineList")
    public ResponseEntity<?> getPolicyLines(@RequestParam UUID documentId) {
        List<ApprovalPolicyLineResponseDto> result = policyLineService.getPolicyLines(documentId);
        return new ResponseEntity<>(
                ApiResponse.success(result, "결재 정책 라인 목록 조회 성공"),
                HttpStatus.OK
        );
    }

    @GetMapping("/{policyLineId}")
    public ResponseEntity<?> getPolicyLine(@PathVariable UUID policyLineId) {
        ApprovalPolicyLineResponseDto result = policyLineService.getPolicyLine(policyLineId);
        return new ResponseEntity<>(
                ApiResponse.success(result, "결재 정책 라인 단건 조회 성공"),
                HttpStatus.OK
        );
    }

    /**
     * 결재선 미리보기 API
     * 문서 양식의 정책 라인을 기반으로 실제 결재자 후보 목록을 반환
     * 프론트에서 요청자가 양식 선택 시 호출하여 결재선을 확인/수정할 수 있도록 함
     */
    @GetMapping("/resolve")
    public ResponseEntity<?> resolveApprovalLine(
            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
            @RequestParam UUID documentId) {
        List<ResolvedApprovalLineDto> result =
                resolverService.resolveApprovalLine(documentId, memberPositionId);
        return new ResponseEntity<>(
                ApiResponse.success(result, "결재선 미리보기 조회 성공"),
                HttpStatus.OK
        );
    }
}
