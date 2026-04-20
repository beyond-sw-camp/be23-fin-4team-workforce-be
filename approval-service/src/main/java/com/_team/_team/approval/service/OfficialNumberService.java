package com._team._team.approval.service;

import com._team._team.approval.domain.ApprovalRequest;
import com._team._team.approval.repository.ApprovalRequestRepository;
import com._team._team.dto.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class OfficialNumberService {

    private final ApprovalRequestRepository approvalRequestRepository;

    @Autowired
    public OfficialNumberService(ApprovalRequestRepository approvalRequestRepository) {
        this.approvalRequestRepository = approvalRequestRepository;
    }

    /**
     * 공문 번호 생성: {부서명 앞 2글자}-{YYYY}-{순번 4자리}
     * 예: 인사-2026-0147
     *
     * - 년도별, 부서별 APPROVED 상태 공문 수 + 1
     * - DB unique 제약으로 중복 방지, 충돌 시 @Retryable 로 재시도
     */
    @Retryable(
            retryFor = DataIntegrityViolationException.class,
            maxAttempts = 5,
            backoff = @Backoff(delay = 50, multiplier = 1.5)
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generate(UUID requestId) {
        // REQUIRES_NEW 트랜잭션에서 독립적으로 조회
        ApprovalRequest request = approvalRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "결재 요청을 찾을 수 없습니다."));

        int year = LocalDate.now().getYear();
        String prefix = resolvePrefix(request.getRequesterOrganizationName());

        long approvedCount = approvalRequestRepository.countOfficialApprovedInYear(
                request.getRequesterOrganizationId(), year);

        long seq = approvedCount + 1;
        String number = String.format("%s-%d-%04d", prefix, year, seq);

        request.updateDocumentNumber(number);
        approvalRequestRepository.save(request);

        log.info("공문 번호 생성: requestId={}, number={}", requestId, number);
        return number;
    }

    private String resolvePrefix(String orgName) {
        if (orgName == null || orgName.isBlank()) {
            return "공문";
        }
        String trimmed = orgName.trim();
        return trimmed.length() >= 2 ? trimmed.substring(0, 2) : trimmed;
    }
}
