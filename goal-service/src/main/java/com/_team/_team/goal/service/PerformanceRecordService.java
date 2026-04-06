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

    @Transactional
    public PerformanceRecordResDto submitRecord(UUID goalId, PerformanceRecordCreateReqDto dto) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "목표를 찾을 수 없습니다. id=" + goalId));

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
                .submittedBy(dto.getSubmittedBy())
                .build();

        PerformanceRecord saved = performanceRecordRepository.save(record);
        return PerformanceRecordResDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<PerformanceRecordResDto> listRecords(UUID goalId) {
        return performanceRecordRepository.findByGoal_GoalIdOrderByCreatedAtDesc(goalId)
                .stream()
                .map(PerformanceRecordResDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public PerformanceRecordResDto reviewRecord(UUID performanceRecordId, PerformanceRecordReviewReqDto dto) {
        PerformanceRecord record = performanceRecordRepository.findById(performanceRecordId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "실적 기록을 찾을 수 없습니다. id=" + performanceRecordId));

        if (record.getReviewStatus() != ReviewStatus.PENDING) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "대기(PENDING) 상태의 실적만 검토할 수 있습니다. 현재 상태: " + record.getReviewStatus());
        }

        BigDecimal beforeValue = record.getActualValue();
        String beforeDesc = record.getDescription();

        if (Boolean.TRUE.equals(dto.getConfirmed())) {
            record.confirm(dto.getReviewedBy(), dto.getConvertedScore());
            // 실적 확인 시 목표의 실제값 및 달성률 갱신
            if (record.getActualValue() != null) {
                record.getGoal().updateActualValue(record.getActualValue());
            }
        } else {
            record.reject(dto.getReviewedBy(), dto.getRejectReason());
        }

        PerformanceRecordHistory history = PerformanceRecordHistory.builder()
                .performanceRecord(record)
                .beforeValue(beforeValue)
                .afterValue(record.getActualValue())
                .beforeDesc(beforeDesc)
                .afterDesc(record.getDescription())
                .changeReason(Boolean.TRUE.equals(dto.getConfirmed()) ? "실적 확인" : "실적 반려: " + dto.getRejectReason())
                .changedBy(dto.getReviewedBy())
                .build();

        performanceRecordHistoryRepository.save(history);

        return PerformanceRecordResDto.from(record);
    }

    @Transactional(readOnly = true)
    public List<PerformanceRecordHistoryResDto> getHistory(UUID performanceRecordId) {
        return performanceRecordHistoryRepository
                .findByPerformanceRecord_PerformanceRecordIdOrderByChangedAtDesc(performanceRecordId)
                .stream()
                .map(PerformanceRecordHistoryResDto::from)
                .collect(Collectors.toList());
    }
}
