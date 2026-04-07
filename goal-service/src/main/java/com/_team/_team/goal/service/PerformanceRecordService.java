package com._team._team.goal.service;

import com._team._team.dto.BusinessException;
import com._team._team.goal.domain.Goal;
import com._team._team.goal.domain.PerformanceRecord;
import com._team._team.goal.domain.PerformanceRecordHistory;
import com._team._team.goal.domain.enums.GoalStatus;
import com._team._team.goal.domain.enums.ReviewStatus;
import com._team._team.goal.dto.reqdto.PerformanceRecordCreateReqDto;
import com._team._team.goal.dto.reqdto.PerformanceRecordReviewReqDto;
import com._team._team.goal.dto.resdto.PerformanceRecordHistoryResDto;
import com._team._team.goal.dto.resdto.PerformanceRecordResDto;
import com._team._team.goal.repository.GoalRepository;
import com._team._team.goal.repository.PerformanceRecordHistoryRepository;
import com._team._team.goal.repository.PerformanceRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PerformanceRecordService {

    final PerformanceRecordRepository performanceRecordRepository;
    final PerformanceRecordHistoryRepository performanceRecordHistoryRepository;
    final GoalRepository goalRepository;

    @Autowired
    public PerformanceRecordService(PerformanceRecordRepository performanceRecordRepository,
                                    PerformanceRecordHistoryRepository performanceRecordHistoryRepository,
                                    GoalRepository goalRepository) {
        this.performanceRecordRepository = performanceRecordRepository;
        this.performanceRecordHistoryRepository = performanceRecordHistoryRepository;
        this.goalRepository = goalRepository;
    }

    // 실적 제출
    @Transactional
    public PerformanceRecordResDto submitRecord(UUID goalId, PerformanceRecordCreateReqDto dto, UUID submittedBy, UUID companyId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "목표를 찾을 수 없습니다. id=" + goalId));

        // 회사 격리 확인
        if (!goal.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "타사의 목표에 실적을 제출할 수 없습니다.");
        }

        // 소유자 확인 (본인의 목표에만 제출 가능)
        if (!goal.getOwnerId().equals(submittedBy)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "자신의 목표에만 실적을 제출할 수 있습니다.");
        }

        if (goal.getStatus() != GoalStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "활성(ACTIVE) 상태의 목표에만 실적을 제출할 수 있습니다. 현재 상태: " + goal.getStatus());
        }

        PerformanceRecord record = PerformanceRecord.builder()
                .goal(goal)
                .inputType(dto.getInputType())
                .actualValue(dto.getActualValue())
                .description(dto.getDescription())
                .selfScore(dto.getSelfScore())
                .submittedBy(submittedBy)
                .build();

        PerformanceRecord saved = performanceRecordRepository.save(record);
        return PerformanceRecordResDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<PerformanceRecordResDto> listRecords(UUID goalId, UUID companyId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "목표를 찾을 수 없습니다. id=" + goalId));

        // 회사 격리 확인
        if (!goal.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "타사의 실적 목록을 조회할 수 없습니다.");
        }

        return performanceRecordRepository.findByGoal_GoalIdOrderByCreatedAtDesc(goalId)
                .stream()
                .map(PerformanceRecordResDto::from)
                .collect(Collectors.toList());
    }

    // 실적 검토
    @Transactional
    public PerformanceRecordResDto reviewRecord(UUID goalId, UUID performanceRecordId,
                                                PerformanceRecordReviewReqDto dto, UUID reviewedBy, UUID companyId) {
        PerformanceRecord record = performanceRecordRepository.findById(performanceRecordId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "실적 기록을 찾을 수 없습니다. id=" + performanceRecordId));

        // URL의 goalId와 record가 실제로 속한 goalId 일치 검증
        if (!record.getGoal().getGoalId().equals(goalId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "해당 실적 기록은 요청한 목표에 속하지 않습니다.");
        }

        // 회사 격리 확인
        if (!record.getGoal().getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "타사의 실적을 검토할 수 없습니다.");
        }

        // TODO: 매니저 권한 확인 로직 추가 가능 (record.getGoal().getOwnerId()의 매니저가 reviewedBy인지 확인)

        if (record.getReviewStatus() != ReviewStatus.PENDING) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "대기(PENDING) 상태의 실적만 검토할 수 있습니다. 현재 상태: " + record.getReviewStatus());
        }

        BigDecimal beforeValue = record.getActualValue();
        String beforeDesc = record.getDescription();

        if (Boolean.TRUE.equals(dto.getConfirmed())) {
            record.confirm(reviewedBy, dto.getConvertedScore());
            // 실적 확인 시 목표의 실제값 및 달성률 동기화 (이미 Goal.updateActualValue가 정교한 계산을 수행함)
            if (record.getActualValue() != null) {
                record.getGoal().updateActualValue(record.getActualValue());
            }
        } else {
            record.reject(reviewedBy, dto.getRejectReason());
            // 반려 시에는 가장 최근에 승인된(APPROVED) 실적으로 롤백하는 로직을 추가할 수 있음
            // 현재는 최신 제출분 기준이므로 상태만 반려로 변경
        }

        PerformanceRecordHistory history = PerformanceRecordHistory.builder()
                .performanceRecord(record)
                .beforeValue(beforeValue)
                .afterValue(record.getActualValue())
                .beforeDesc(beforeDesc)
                .afterDesc(record.getDescription())
                .changeReason(Boolean.TRUE.equals(dto.getConfirmed()) ? "실적 확인" : "실적 반려: " + dto.getRejectReason())
                .changedBy(reviewedBy)
                .build();

        performanceRecordHistoryRepository.save(history);

        return PerformanceRecordResDto.from(record);
    }

    // 실적 이력 조회
    @Transactional(readOnly = true)
    public List<PerformanceRecordHistoryResDto> getHistory(UUID goalId, UUID performanceRecordId, UUID companyId) {
        PerformanceRecord record = performanceRecordRepository.findById(performanceRecordId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "실적 기록을 찾을 수 없습니다. id=" + performanceRecordId));

        // URL의 goalId와 record가 실제로 속한 goalId 일치 검증
        if (!record.getGoal().getGoalId().equals(goalId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "해당 실적 기록은 요청한 목표에 속하지 않습니다.");
        }

        // 회사 격리 확인
        if (!record.getGoal().getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "타사의 실적 이력을 조회할 수 없습니다.");
        }

        return performanceRecordHistoryRepository
                .findByPerformanceRecord_PerformanceRecordIdOrderByChangedAtDesc(performanceRecordId)
                .stream()
                .map(PerformanceRecordHistoryResDto::from)
                .collect(Collectors.toList());
    }
}