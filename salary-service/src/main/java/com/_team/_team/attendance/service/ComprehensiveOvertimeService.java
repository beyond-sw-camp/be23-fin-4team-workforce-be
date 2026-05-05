package com._team._team.attendance.service;

import com._team._team.attendance.dto.resDto.ComprehensiveOvertimeStatusResDto;
import com._team._team.attendance.repository.OvertimeRequestRepositoryQuerydsl.MemberApprovedMinutesRow;
import com._team._team.attendance.repository.OvertimeRequestRepository;
import com._team._team.salary.domain.Salary;
import com._team._team.salary.domain.SalaryPolicy;
import com._team._team.salary.domain.enums.WageSystemType;
import com._team._team.salary.feignClients.MemberFeignClient;
import com._team._team.salary.feignClients.dto.MemberResDto;
import com._team._team.salary.repository.SalaryPolicyRepository;
import com._team._team.salary.repository.SalaryRepository;
import com._team._team.salary.service.CachedMemberLookupService;
import com._team._team.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 포괄임금제 직원 월별 OT 누적 vs 고정 한도 현황 조회
 * - 관리자 [초과 근무 현황] 화면용
 * - 회사+기준일을 키로 5분 캐싱
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComprehensiveOvertimeService {

    private final SalaryRepository salaryRepository;
    private final SalaryPolicyRepository salaryPolicyRepository;
    private final OvertimeRequestRepository overtimeRequestRepository;
    private final MemberFeignClient memberFeignClient;
    private final CachedMemberLookupService cachedMemberLookup;

    /**
     * 회사 전체 포괄임금제 직원 -> 기준일 기준 월간 초과근무시간 누적 현황
     * 기준일로 속한 월의 1일 ~ 말일까지 합산
     * 사용률 50% 미만은 결과에서 제외 (화면 정책)
     */
    @Transactional(readOnly = true)
    public List<ComprehensiveOvertimeStatusResDto> getStatus(UUID companyId, LocalDate baseDate) {
        YearMonth ym = YearMonth.from(baseDate);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();

        // 1. 회사 활성 SalaryPolicy 중 포괄임금제만 필터
        List<SalaryPolicy> activePolicies = salaryPolicyRepository.findActivePolicies(companyId, baseDate);
        Map<UUID, Integer> compPolicyFixed = activePolicies.stream()
                .filter(p -> p.getWageSystemType() == WageSystemType.COMPREHENSIVE)
                .filter(p -> p.getFixedOvertimeMinutes() != null && p.getFixedOvertimeMinutes() > 0)
                .collect(Collectors.toMap(SalaryPolicy::getSalaryPolicyId,
                        SalaryPolicy::getFixedOvertimeMinutes,
                        (a, b) -> a));
        if (compPolicyFixed.isEmpty()) return Collections.emptyList();

        // 2. 그 정책들에 묶인 직원 Salary 조회 - 직원별 fixedLimit
        List<MemberResDto> members = safeGetMembers(companyId);
        if (members.isEmpty()) return Collections.emptyList();
        List<UUID> memberIds = members.stream().map(MemberResDto::getMemberId).toList();

        List<Salary> activeSalaries = salaryRepository
                .findActiveSalariesByMemberIds(companyId, memberIds, baseDate);
        // memberId -> fixedLimit (포괄임금제 정책에 묶여 있는 직원만)
        Map<UUID, Integer> memberFixedLimit = activeSalaries.stream()
                .filter(s -> compPolicyFixed.containsKey(s.getSalaryPolicyId()))
                .collect(Collectors.toMap(Salary::getMemberId,
                        s -> compPolicyFixed.get(s.getSalaryPolicyId()),
                        (a, b) -> a));
        if (memberFixedLimit.isEmpty()) return Collections.emptyList();

        // 3. 회사 단위 월간 승인 초과근무시간 분 집계 - QueryDSL
        Map<UUID, Long> approvedByMember = overtimeRequestRepository
                .sumApprovedMinutesByCompanyAndRange(companyId, from, to)
                .stream()
                .collect(Collectors.toMap(MemberApprovedMinutesRow::memberId,
                        MemberApprovedMinutesRow::sumMinutes));

        // 4. 멤버 정보 결합 + 사용률 50% 이상만 필터, 내림차순
        Map<UUID, MemberResDto> memberMap = members.stream()
                .collect(Collectors.toMap(MemberResDto::getMemberId, m -> m, (a, b) -> a));

        return memberFixedLimit.entrySet().stream()
                .map(e -> {
                    UUID mId = e.getKey();
                    int limit = e.getValue();
                    int approved = approvedByMember.getOrDefault(mId, 0L).intValue();
                    double pct = limit > 0 ? (approved * 100.0 / limit) : 0.0;
                    int exceed = Math.max(0, approved - limit);
                    MemberResDto m = memberMap.get(mId);
                    return ComprehensiveOvertimeStatusResDto.builder()
                            .memberId(mId)
                            .name(m != null ? m.getName() : null)
                            .organizationName(m != null ? m.getOrganizationName() : null)
                            .approvedMinutes(approved)
                            .fixedLimit(limit)
                            .usagePercent(Math.round(pct * 10.0) / 10.0)
                            .exceedMinutes(exceed)
                            .build();
                })
                .filter(d -> d.getUsagePercent() != null && d.getUsagePercent() >= 50.0)
                .sorted((a, b) -> Double.compare(b.getUsagePercent(), a.getUsagePercent()))
                .toList();
    }

    /**
     * 본인 포괄임금제 OT 현황 (단건)
     */
    @Transactional(readOnly = true)
    public ComprehensiveOvertimeStatusResDto getMyStatus(UUID companyId, UUID memberId, LocalDate baseDate) {
        return getStatus(companyId, baseDate).stream()
                .filter(d -> memberId.equals(d.getMemberId()))
                .findFirst()
                .orElse(null);
    }

    private List<MemberResDto> safeGetMembers(UUID companyId) {
        // Redis 캐시(5분 TTL)
        return cachedMemberLookup.getMembersByCompany(companyId);
    }
}
