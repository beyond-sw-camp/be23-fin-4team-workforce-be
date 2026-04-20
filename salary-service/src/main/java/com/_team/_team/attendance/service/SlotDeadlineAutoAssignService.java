package com._team._team.attendance.service;

import com._team._team.attendance.domain.FlexibleTimeSlot;
import com._team._team.attendance.domain.WorkSchedule;
import com._team._team.attendance.domain.enums.ScheduleApprovalStatus;
import com._team._team.attendance.domain.enums.WorkType;
import com._team._team.attendance.repository.FlexibleTimeSlotRepository;
import com._team._team.attendance.repository.MemberScheduleSelectionRepository;
import com._team._team.attendance.repository.WorkScheduleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 스케줄 슬롯 선택 마감일 경과 시 기본 슬롯 자동 할당 서비스
 * 대상 월은 다음 달
 */
@Slf4j
@Service
public class SlotDeadlineAutoAssignService {

    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final UUID SYSTEM_ACTOR = new UUID(0L, 0L);
    private static final String ACTIVE = "Y";

    private final WorkScheduleRepository workScheduleRepository;
    private final FlexibleTimeSlotRepository flexibleTimeSlotRepository;
    private final MemberScheduleSelectionRepository memberScheduleSelectionRepository;
    private final MemberScheduleSelectionService memberScheduleSelectionService;

    @Autowired
    public SlotDeadlineAutoAssignService(
            WorkScheduleRepository workScheduleRepository,
            FlexibleTimeSlotRepository flexibleTimeSlotRepository,
            MemberScheduleSelectionRepository memberScheduleSelectionRepository,
            MemberScheduleSelectionService memberScheduleSelectionService) {
        this.workScheduleRepository = workScheduleRepository;
        this.flexibleTimeSlotRepository = flexibleTimeSlotRepository;
        this.memberScheduleSelectionRepository = memberScheduleSelectionRepository;
        this.memberScheduleSelectionService = memberScheduleSelectionService;
    }

    /**
     * targetDate 기준 마감일 경과 자동 할당 실행
     * 오늘이 4/26 이면 마감일이 25일 인 회사 대상, 5월 슬롯 할당
     */
    @Transactional
    public AssignResult runForDate(LocalDate targetDate) {
        // 오늘이 어제인 회사만 대상
        int deadlineDay = targetDate.minusDays(1).getDayOfMonth();

        List<WorkSchedule> targetSchedules = workScheduleRepository
                .findActiveFlexibleWithDeadlineDay(deadlineDay);

        // 다음 달 YYYY-MM
        String targetYearMonth = YearMonth.from(targetDate).plusMonths(1).format(YM);

        int assignedCount = 0;

        for (WorkSchedule schedule : targetSchedules) {
            assignedCount += assignForSchedule(schedule, targetYearMonth);
        }

        log.info("[SlotDeadline] targetDate={} 대상스케줄={} 할당={}",
                targetDate, targetSchedules.size(), assignedCount);

        return new AssignResult(targetSchedules.size(), assignedCount);
    }

    // 특정 스케줄의 기본 스케줄 슬롯을 미선택 직원에게 자동 할당
    private int assignForSchedule(WorkSchedule schedule, String targetYearMonth) {
        FlexibleTimeSlot defaultSlot = flexibleTimeSlotRepository
                .findByWorkScheduleIdAndIsDefaultAndActiveYn(
                        schedule.getWorkScheduleId(), true, ACTIVE)
                .orElse(null);

        if (defaultSlot == null) {
            log.warn("[SlotDeadline] 기본 스케줄 슬롯 없음 workScheduleId={}",
                    schedule.getWorkScheduleId());
            return 0;
        }

        // 이미 해당 월에 유효 선택 있는 직원 목록
        Set<UUID> alreadySelected = new HashSet<>(
                memberScheduleSelectionRepository.findMembersWithActiveSelection(
                        schedule.getCompanyId(), targetYearMonth));

        // TODO member-service Feign 으로 회사 활성 직원 목록 조회 필요
        // 현재는 임시로 빈 리스트 반환, 실제 연동 시 아래 주석 해제
        // List<UUID> allMembers = memberServiceClient.findActiveMembers(schedule.getCompanyId());
        List<UUID> allMembers = List.of();

        int assigned = 0;
        for (UUID memberId : allMembers) {
            if (alreadySelected.contains(memberId)) continue;

            memberScheduleSelectionService.autoAssignDefault(
                    schedule.getCompanyId(),
                    memberId,
                    targetYearMonth,
                    defaultSlot.getSlotId(),
                    SYSTEM_ACTOR);
            assigned++;
        }

        return assigned;
    }

    public record AssignResult(int targetSchedules, int assignedMembers) {}
}
