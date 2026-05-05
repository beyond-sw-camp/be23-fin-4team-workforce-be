package com._team._team.attendance.service;

import com._team._team.attendance.domain.OvertimePolicy;
import com._team._team.attendance.dto.resDto.OvertimeUsageResDto;
import com._team._team.attendance.repository.DailyAttendanceRepository;
import com._team._team.attendance.repository.DailyAttendanceRepository.MemberOvertimeMinutesRow;
import com._team._team.attendance.repository.OvertimePolicyRepository;
import com._team._team.attendance.repository.OvertimeRequestRepositoryQuerydsl.MemberApprovedMinutesRow;
import com._team._team.attendance.repository.OvertimeRequestRepository;
import com._team._team.salary.feignClients.dto.MemberResDto;
import com._team._team.salary.service.CachedMemberLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 직원 월별 초과근무시간 누적 + 회사 월 한도 (주52시간/월한도) 모니터링
 * 관리자 [초과 근무 현황] 화면용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OvertimeUsageService {

    private final OvertimePolicyRepository overtimePolicyRepository;
    private final OvertimeRequestRepository overtimeRequestRepository;
    private final DailyAttendanceRepository dailyAttendanceRepository;
    private final CachedMemberLookupService cachedMemberLookup;

    /**
     * 회사 전체 직원 -> 기준일이 속한 월의 1일 ~ 말일 범위 누적 OT 현황
     */
    @Transactional(readOnly = true)
    public List<OvertimeUsageResDto> getStatus(UUID companyId, LocalDate baseDate) {
        YearMonth ym = YearMonth.from(baseDate);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();

        // 회사 활성 OvertimePolicy - 월 한도 추출 (없으면 null)
        Integer monthlyLimit = overtimePolicyRepository
                .findByCompanyIdAndEffectiveToIsNull(companyId)
                .map(OvertimePolicy::getMonthlyOvertimeLimitMinutes)
                .orElse(null);

        // 멤버 목록
        List<MemberResDto> members = cachedMemberLookup.getMembersByCompany(companyId);
        if (members.isEmpty()) return Collections.emptyList();
        Map<UUID, MemberResDto> memberMap = members.stream()
                .collect(Collectors.toMap(MemberResDto::getMemberId, m -> m, (a, b) -> a));

        // 실측 초과근무시간 합
        Map<UUID, Long> actualByMember = dailyAttendanceRepository
                .sumActualOvertimeByCompanyAndRange(companyId, from, to)
                .stream()
                .collect(Collectors.toMap(MemberOvertimeMinutesRow::getMemberId,
                        MemberOvertimeMinutesRow::getSumMinutes,
                        (a, b) -> a));

        // 승인된 초과근무시간 합
        Map<UUID, Long> approvedByMember = overtimeRequestRepository
                .sumApprovedMinutesByCompanyAndRange(companyId, from, to)
                .stream()
                .collect(Collectors.toMap(MemberApprovedMinutesRow::memberId,
                        MemberApprovedMinutesRow::sumMinutes,
                        (a, b) -> a));

        // 두 집계 중 한쪽이라도 값이 있는 멤버 union
        Set<UUID> memberIds = new HashSet<>();
        memberIds.addAll(actualByMember.keySet());
        memberIds.addAll(approvedByMember.keySet());

        return memberIds.stream()
                .map(mId -> {
                    int actual = actualByMember.getOrDefault(mId, 0L).intValue();
                    int approved = approvedByMember.getOrDefault(mId, 0L).intValue();
                    int limit = monthlyLimit != null ? monthlyLimit : 0;
                    double pct = limit > 0 ? (actual * 100.0 / limit) : 0.0;
                    int exceed = limit > 0 ? Math.max(0, actual - limit) : 0;
                    MemberResDto m = memberMap.get(mId);
                    return OvertimeUsageResDto.builder()
                            .memberId(mId)
                            .name(m != null ? m.getName() : null)
                            .organizationName(m != null ? m.getOrganizationName() : null)
                            .actualOvertimeMinutes(actual)
                            .approvedMinutes(approved)
                            .fixedLimit(monthlyLimit)
                            .usagePercent(Math.round(pct * 10.0) / 10.0)
                            .exceedMinutes(exceed)
                            .build();
                })
                .sorted((a, b) -> Double.compare(
                        b.getUsagePercent() != null ? b.getUsagePercent() : 0.0,
                        a.getUsagePercent() != null ? a.getUsagePercent() : 0.0))
                .toList();
    }

    /**
     * 본인 OT 현황 (단건)
     */
    @Transactional(readOnly = true)
    public OvertimeUsageResDto getMyStatus(UUID companyId, UUID memberId, LocalDate baseDate) {
        return getStatus(companyId, baseDate).stream()
                .filter(d -> memberId.equals(d.getMemberId()))
                .findFirst()
                .orElse(null);
    }
}
