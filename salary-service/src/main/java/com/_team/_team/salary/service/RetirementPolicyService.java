package com._team._team.salary.service;

import com._team._team.dto.BusinessException;
import com._team._team.salary.domain.RetirementPolicy;
import com._team._team.salary.domain.enums.RetirementType;
import com._team._team.salary.dto.reqdto.RetirementPolicyCreateReqDto;
import com._team._team.salary.dto.reqdto.RetirementPolicyUpdateReqDto;
import com._team._team.salary.dto.resdto.RetirementPolicyResDto;
import com._team._team.salary.repository.RetirementPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// 회사별 퇴직급여 제도 정책 관리
@Slf4j
@Service
@Transactional
public class RetirementPolicyService {

    private final RetirementPolicyRepository retirementPolicyRepository;

    @Autowired
    public RetirementPolicyService(RetirementPolicyRepository retirementPolicyRepository) {
        this.retirementPolicyRepository = retirementPolicyRepository;
    }

    // 정책 등록 이전 활성 정책 자동 마감
    public RetirementPolicyResDto create(UUID companyId, RetirementPolicyCreateReqDto reqDto) {
        validatePeriod(reqDto.getEffectiveFrom(), reqDto.getEffectiveTo());

        retirementPolicyRepository.findActiveAt(companyId, reqDto.getEffectiveFrom())
                .ifPresent(prev -> {
                    LocalDate closingDate = reqDto.getEffectiveFrom().minusDays(1);
                    if (!prev.getEffectiveFrom().isAfter(closingDate)) {
                        prev.closeAt(closingDate);
                        log.info("[RETIREMENT-POLICY] 이전 활성 정책 자동 마감 companyId={} prevId={} closeAt={}",
                                companyId, prev.getRetirementPolicyId(), closingDate);
                    }
                });

        RetirementPolicy saved = retirementPolicyRepository.save(RetirementPolicy.builder()
                .companyId(companyId)
                .retirementType(reqDto.getRetirementType())
                .effectiveFrom(reqDto.getEffectiveFrom())
                .effectiveTo(reqDto.getEffectiveTo())
                .memo(reqDto.getMemo())
                .build());

        log.info("[RETIREMENT-POLICY] 등록 companyId={} policyId={} type={}",
                companyId, saved.getRetirementPolicyId(), saved.getRetirementType());
        return RetirementPolicyResDto.fromEntity(saved);
    }

    // 정책 수정 시작일 변경 불가 제도 종료일 메모 만 가능
    public RetirementPolicyResDto update(UUID companyId, UUID policyId,
                                         RetirementPolicyUpdateReqDto reqDto) {
        RetirementPolicy policy = findOrThrow(policyId);
        validateCompany(policy, companyId);
        validatePeriod(policy.getEffectiveFrom(), reqDto.getEffectiveTo());

        policy.update(reqDto.getRetirementType(), reqDto.getEffectiveTo(), reqDto.getMemo());
        log.info("[RETIREMENT-POLICY] 수정 companyId={} policyId={} type={}",
                companyId, policyId, policy.getRetirementType());
        return RetirementPolicyResDto.fromEntity(policy);
    }

    // 정책 소프트 삭제
    public void delete(UUID companyId, UUID policyId) {
        RetirementPolicy policy = findOrThrow(policyId);
        validateCompany(policy, companyId);
        policy.softDelete();
        log.info("[RETIREMENT-POLICY] 삭제 companyId={} policyId={}", companyId, policyId);
    }

    // 회사 전체 정책 이력 조회
    @Transactional(readOnly = true)
    public List<RetirementPolicyResDto> listByCompany(UUID companyId) {
        return retirementPolicyRepository
                .findByCompanyIdAndDelYnOrderByEffectiveFromDesc(companyId, "N")
                .stream()
                .map(RetirementPolicyResDto::fromEntity)
                .toList();
    }

    // 활성 정책 조회 없으면 기본 LEGAL 자동 생성
    public RetirementPolicyResDto getOrCreateActive(UUID companyId) {
        return retirementPolicyRepository.findActiveAt(companyId, LocalDate.now())
                .map(RetirementPolicyResDto::fromEntity)
                .orElseGet(() -> {
                    log.warn("[RETIREMENT-POLICY] 활성 정책 없음 기본 LEGAL 자동 생성 companyId={}",
                            companyId);
                    RetirementPolicy created = retirementPolicyRepository.save(
                            RetirementPolicy.builder()
                                    .companyId(companyId)
                                    .retirementType(RetirementType.LEGAL)
                                    .effectiveFrom(LocalDate.now())
                                    .memo("활성 정책 없음 자동 생성 기본 LEGAL")
                                    .build());
                    return RetirementPolicyResDto.fromEntity(created);
                });
    }

    // 활성 정책 엔티티 직접 조회 시뮬레이션 등 다른 서비스에서 사용
    @Transactional(readOnly = true)
    public RetirementPolicy findActiveEntity(UUID companyId, LocalDate date) {
        return retirementPolicyRepository.findActiveAt(companyId, date)
                .orElse(null);
    }

    // 기간 정합성 검증
    private void validatePeriod(LocalDate from, LocalDate to) {
        if (from == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "적용 시작일은 필수입니다");
        }
        if (to != null && to.isBefore(from)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "적용 종료일은 시작일 이후여야 합니다");
        }
    }

    private RetirementPolicy findOrThrow(UUID policyId) {
        return retirementPolicyRepository.findById(policyId)
                .filter(p -> "N".equals(p.getDelYn()))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "퇴직급여 정책을 찾을 수 없습니다"));
    }

    private void validateCompany(RetirementPolicy policy, UUID companyId) {
        if (!policy.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "다른 회사의 정책에 접근할 수 없습니다");
        }
    }
}