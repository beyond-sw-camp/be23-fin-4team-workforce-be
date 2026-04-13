package com._team._team.approval.service;

import com._team._team.approval.domain.ApprovalDocument;
import com._team._team.approval.domain.enums.RequestType;
import com._team._team.approval.dto.reqdto.ApprovalDocumentCreateReqDto;
import com._team._team.approval.dto.resdto.ApprovalDocumentResDto;
import com._team._team.approval.repository.ApprovalDocumentRepository;
import com._team._team.dto.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ApprovalDocumentService {

    private final ApprovalDocumentRepository approvalDocumentRepository;

    @Autowired
    public ApprovalDocumentService(ApprovalDocumentRepository approvalDocumentRepository) {
        this.approvalDocumentRepository = approvalDocumentRepository;
    }

    public void initDefaultDocuments(UUID companyId) {
        // 중복 방지 - 이미 해당 회사 양식이 있으면 건너뛰기
        if (!approvalDocumentRepository.findByCompanyIdAndIsActiveYn(companyId, "Y").isEmpty()) {
            return;
        }

        // 1. 연차신청서
        approvalDocumentRepository.save(ApprovalDocument.builder()
                .companyId(companyId)
                .documentName("연차신청서")
                .requestType(RequestType.VACATION)
                .formSchema("{\"fields\":[{\"name\":\"vacationType\",\"label\":\"휴가 종류\",\"type\":\"select\",\"options\":[\"연차\",\"반차(오전)\",\"반차(오후)\"]},{\"name\":\"startDate\",\"label\":\"시작일\",\"type\":\"date\"},{\"name\":\"endDate\",\"label\":\"종료일\",\"type\":\"date\"},{\"name\":\"reason\",\"label\":\"사유\",\"type\":\"textarea\"}]}")
                .build());

        // 2. 지출결의서
        approvalDocumentRepository.save(ApprovalDocument.builder()
                .companyId(companyId)
                .documentName("지출결의서")
                .requestType(RequestType.GENERAL)
                .formSchema("{\"fields\":[{\"name\":\"title\",\"label\":\"제목\",\"type\":\"text\"},{\"name\":\"amount\",\"label\":\"금액\",\"type\":\"number\"},{\"name\":\"purpose\",\"label\":\"지출 목적\",\"type\":\"textarea\"},{\"name\":\"paymentDate\",\"label\":\"지출일\",\"type\":\"date\"}]}")
                .build());

        // 3. 업무보고서
        approvalDocumentRepository.save(ApprovalDocument.builder()
                .companyId(companyId)
                .documentName("업무보고서")
                .requestType(RequestType.GENERAL)
                .formSchema("{\"fields\":[{\"name\":\"title\",\"label\":\"제목\",\"type\":\"text\"},{\"name\":\"content\",\"label\":\"보고 내용\",\"type\":\"textarea\"},{\"name\":\"reportDate\",\"label\":\"보고일\",\"type\":\"date\"}]}")
                .build());
    }



    // 결재 양식 생성
    public ApprovalDocumentResDto create(UUID companyId, ApprovalDocumentCreateReqDto reqDto) {
        ApprovalDocument document = reqDto.toEntity(companyId);
        ApprovalDocument saved = approvalDocumentRepository.save(document);
        return ApprovalDocumentResDto.fromEntity(saved);
    }

    // 결재 양식 단건 조회
    @Transactional(readOnly = true)
    public ApprovalDocumentResDto findById(UUID companyId, UUID documentId) {
        ApprovalDocument document = approvalDocumentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "결재 양식을 찾을 수 없습니다."));

        if (!document.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        return ApprovalDocumentResDto.fromEntity(document);
    }

    // 회사별 활성 양식 목록 조회
    @Transactional(readOnly = true)
    public List<ApprovalDocumentResDto> findActiveByCompanyId(UUID companyId) {
        return approvalDocumentRepository
                .findByCompanyIdAndIsActiveYn(companyId, "Y")
                .stream()
                .map(ApprovalDocumentResDto::fromEntity)
                .toList();
    }

    // 회사별 전체 양식 목록 조회 (관리자용)
    @Transactional(readOnly = true)
    public List<ApprovalDocumentResDto> findAllByCompanyId(UUID companyId) {
        return approvalDocumentRepository
                .findByCompanyId(companyId)
                .stream()
                .map(ApprovalDocumentResDto::fromEntity)
                .toList();
    }

    // 결재 양식 활성화
    public ApprovalDocumentResDto activate(UUID companyId, UUID documentId) {
        ApprovalDocument document = approvalDocumentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "결재 양식을 찾을 수 없습니다."));

        if (!document.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        document.activate();
        return ApprovalDocumentResDto.fromEntity(document);
    }

    // 결재 양식 비활성화
    public ApprovalDocumentResDto deactivate(UUID companyId, UUID documentId) {
        ApprovalDocument document = approvalDocumentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "결재 양식을 찾을 수 없습니다."));

        if (!document.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        document.deactivate();
        return ApprovalDocumentResDto.fromEntity(document);
    }

    public ApprovalDocumentResDto enableAutoApprove(UUID companyId, UUID documentId) {
        ApprovalDocument document = approvalDocumentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "결재 양식을 찾을 수 없습니다."));

        if (!document.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        document.enableAutoApprove();
        return ApprovalDocumentResDto.fromEntity(document);
    }

    public ApprovalDocumentResDto disableAutoApprove(UUID companyId, UUID documentId) {
        ApprovalDocument document = approvalDocumentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "결재 양식을 찾을 수 없습니다."));

        if (!document.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        document.disableAutoApprove();
        return ApprovalDocumentResDto.fromEntity(document);
    }
}
