package com._team._team.service;

import com._team._team.domain.ApprovalDocument;
import com._team._team.dtos.reqdto.ApprovalDocumentCreateDto;
import com._team._team.repository.ApprovalDocumentRepository;
import com._team._team.repository.ApprovalPolicyLineRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class ApprovalDocumentService {

    private final ApprovalDocumentRepository documentRepository;
    private final ApprovalPolicyLineRepository approvalPolicyLineRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public ApprovalDocumentService(ApprovalDocumentRepository documentRepository, ApprovalPolicyLineRepository approvalPolicyLineRepository, ObjectMapper objectMapper) {
        this.documentRepository = documentRepository;
        this.approvalPolicyLineRepository = approvalPolicyLineRepository;
        this.objectMapper = objectMapper;
    }

//    결재문서양식 생성
    public void createDocument(ApprovalDocumentCreateDto dto){
        ApprovalDocument approvalDocument = ApprovalDocument.builder()
                .documentName(dto.getDocumentName())
                .requestType(dto.getRequestType())
//                .formSchema(dto.getFormSchema())
                .autoApproveYn(dto.getAutoApproveYn())
                .build();
    }



}
