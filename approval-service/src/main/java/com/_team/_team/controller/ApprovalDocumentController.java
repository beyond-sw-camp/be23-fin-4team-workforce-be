package com._team._team.controller;

import com._team._team.dto.ApiResponse;
import com._team._team.dtos.reqdto.ApprovalDocumentCreateDto;
import com._team._team.service.ApprovalDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/approval")
// url 예시 : http://localhost:8080/member(각서비스이름)/member(리퀘스트메핑)/health(각api)
public class ApprovalDocumentController {

    private final ApprovalDocumentService approvalDocumentService;

    @Autowired
    public ApprovalDocumentController(ApprovalDocumentService approvalDocumentService) {
        this.approvalDocumentService = approvalDocumentService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> documentCreate(@RequestBody ApprovalDocumentCreateDto dto){
        approvalDocumentService.createDocument(dto);
        return new ResponseEntity<>(
                ApiResponse.success(dto, "문서양식 생성 성공"),
                HttpStatus.OK
        );
    }

    @PatchMapping("/enableForm/{formId}")
    public ResponseEntity<?> enableForm(@PathVariable UUID formId){
        approvalDocumentService.formActivate(formId);
        return new ResponseEntity<>(
                ApiResponse.success(formId, "문서양식 활성화 성공"),
                HttpStatus.OK
        );
    }

    @PatchMapping("/disableForm/{formId}")
    public ResponseEntity<?> disableForm(@PathVariable UUID formId){
        approvalDocumentService.formDeactivate(formId);
        return new ResponseEntity<>(
                ApiResponse.success(formId, "문서양식 비활성화 성공"),
                HttpStatus.OK
        );
    }
}
