package com._team._team.approval.controller;

import com._team._team.approval.dtos.reqdto.ApprovalPolicyLineCreateReqDto;
import com._team._team.approval.dtos.resdto.ApprovalPolicyLineResDto;
import com._team._team.approval.service.ApprovalPolicyLineService;
import com._team._team.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/approval/policyLines")
public class ApprovalPolicyLineController {

    private final ApprovalPolicyLineService policyLineService;

    @Autowired
    public ApprovalPolicyLineController(ApprovalPolicyLineService policyLineService) {
        this.policyLineService = policyLineService;
    }

    // 결재라인 정책 일괄 저장
    @PostMapping
    public ResponseEntity<ApiResponse<List<ApprovalPolicyLineResDto>>> savePolicyLines(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @Valid @RequestBody ApprovalPolicyLineCreateReqDto reqDto) {
        List<ApprovalPolicyLineResDto> result = policyLineService.savePolicyLines(companyId, reqDto);
        return new ResponseEntity<>(
                ApiResponse.success(result, "결재라인 정책이 저장되었습니다."),
                HttpStatus.CREATED
        );
    }

    // 양식별 결재라인 정책 조회
    @GetMapping("/{documentId}")
    public ResponseEntity<ApiResponse<List<ApprovalPolicyLineResDto>>> findByDocumentId(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID documentId) {
        List<ApprovalPolicyLineResDto> result = policyLineService.findByDocumentId(companyId, documentId);
        return new ResponseEntity<>(
                ApiResponse.success(result, "결재라인 정책 조회 성공"),
                HttpStatus.OK
        );
    }

    // 양식별 결재라인 정책 전체 삭제
    @DeleteMapping("/{documentId}")
    public ResponseEntity<ApiResponse<Void>> deletePolicyLines(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID documentId) {
        policyLineService.deletePolicyLines(companyId, documentId);
        return new ResponseEntity<>(
                ApiResponse.success(null, "결재라인 정책이 삭제되었습니다."),
                HttpStatus.OK
        );
    }
}
