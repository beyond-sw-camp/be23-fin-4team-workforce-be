package com._team._team.service;

import com._team._team.domain.ApprovalDocument;
import com._team._team.dtos.reqdto.ApprovalDocumentCreateDto;
import com._team._team.repository.ApprovalDocumentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Transactional
public class ApprovalDocumentService {

    private final ApprovalDocumentRepository documentRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public ApprovalDocumentService(ApprovalDocumentRepository documentRepository, ObjectMapper objectMapper) {
        this.documentRepository = documentRepository;
        this.objectMapper = objectMapper;
    }

//    결재문서양식 생성
    public void createDocument(ApprovalDocumentCreateDto dto){
        String formSchemaJson;
        try{
            formSchemaJson = objectMapper.writeValueAsString(dto.getFormSchema());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("formSchema JSON 변환 실패", e);
        }
        ApprovalDocument approvalDocument = ApprovalDocument.builder()
                .companyId(dto.getCompanyId())
                .documentName(dto.getDocumentName())
                .requestType(dto.getRequestType())
                .formSchema(formSchemaJson)
                .autoApproveYn(dto.getAutoApproveYn())
                .build();
        documentRepository.save(approvalDocument);
    }

    public void formActivate(UUID formId){
        ApprovalDocument approvalDocument = documentRepository.findById(formId).orElseThrow(() -> new EntityNotFoundException("해당 문서양식이 존재하지 않습니다."));
        approvalDocument.activate();
    }

    public void formDeactivate(UUID formId){
        ApprovalDocument approvalDocument = documentRepository.findById(formId).orElseThrow(() -> new EntityNotFoundException("해당 문서양식이 존재하지 않습니다."));
        approvalDocument.deactivate();
    }



}
