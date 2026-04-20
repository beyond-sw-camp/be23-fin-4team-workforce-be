package com._team._team.attendance.service;

import com._team._team.attendance.domain.OvertimePolicy;
import com._team._team.attendance.dto.reqDto.OvertimePolicyCreateReqDto;
import com._team._team.attendance.dto.reqDto.OvertimePolicyUpdateReqDto;
import com._team._team.attendance.dto.resDto.OvertimePolicyResDto;
import com._team._team.attendance.repository.OvertimePolicyRepository;
import com._team._team.dto.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 연장근로 정책 관리 서비스
 */
@Service
@Transactional
public class OvertimePolicyService {
    private final OvertimePolicyRepository overtimePolicyRepository;

    @Autowired
    public OvertimePolicyService(OvertimePolicyRepository overtimePolicyRepository) {
        this.overtimePolicyRepository = overtimePolicyRepository;
    }

    /**
     * 정책 신규 생성
     */
    public OvertimePolicyResDto create(UUID companyId, OvertimePolicyCreateReqDto reqDto) {
        overtimePolicyRepository.findByCompanyIdAndEffectiveToIsNull(companyId)
                .ifPresent(existing -> existing.close(reqDto.getEffectiveFrom().minusDays(1)));

        OvertimePolicy saved = overtimePolicyRepository.save(reqDto.toEntity(companyId));
        return OvertimePolicyResDto.fromEntity(saved);
    }

    /**
     * 정책 수정
     * 같은 정책 버전의 내용만 수정, 새 버전을 원하면 create 호출
     */
    public OvertimePolicyResDto update(UUID policyId,
                                       UUID companyId,
                                       OvertimePolicyUpdateReqDto reqDto) {
        OvertimePolicy policy = findPolicy(policyId, companyId);
        policy.update(
                reqDto.getOvertimeRoundingMinutes(),
                reqDto.getApprovalMode(),
                reqDto.getPostApprovalDeadlineHours(),
                reqDto.getWeeklyOvertimeLimitMinutes(),
                reqDto.getWeeklyTotalLimitMinutes(),
                reqDto.getDailyOvertimeLimitMinutes(),
                reqDto.getMonthlyOvertimeLimitMinutes(),
                reqDto.getNightStartTime(),
                reqDto.getNightEndTime(),
                reqDto.getHolidayWorkRequiresApproval()
        );
        return OvertimePolicyResDto.fromEntity(policy);
    }

    /**
     * 현재 적용 중인 정책
     */
    @Transactional(readOnly = true)
    public OvertimePolicyResDto findCurrent(UUID companyId) {
        return overtimePolicyRepository.findByCompanyIdAndEffectiveToIsNull(companyId)
                .map(OvertimePolicyResDto::fromEntity)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "활성 연장근로 정책이 없습니다."));
    }

    /**
     * 단건 조회
     */
    @Transactional(readOnly = true)
    public OvertimePolicyResDto findById(UUID policyId, UUID companyId) {
        return OvertimePolicyResDto.fromEntity(findPolicy(policyId, companyId));
    }

    /**
     * 회사 정책 이력 전체
     */
    @Transactional(readOnly = true)
    public List<OvertimePolicyResDto> findHistory(UUID companyId) {
        return overtimePolicyRepository
                .findAllByCompanyIdOrderByEffectiveFromDesc(companyId)
                .stream()
                .map(OvertimePolicyResDto::fromEntity)
                .collect(Collectors.toList());
    }

    // 정책 찾기
    private OvertimePolicy findPolicy(UUID policyId, UUID companyId) {
        OvertimePolicy policy = overtimePolicyRepository.findById(policyId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "연장근로 정책을 찾을 수 없습니다."));

        if (!policy.getCompanyId().equals(companyId)) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN, "다른 회사의 정책은 접근할 수 없습니다.");
        }
        return policy;
    }
}
