package com._team._team.evaluation.service;

import com._team._team.dto.BusinessException;
import com._team._team.evaluation.domain.CalibrationLog;
import com._team._team.evaluation.domain.Evaluation;
import com._team._team.evaluation.dto.reqdto.CalibrationCreateReqDto;
import com._team._team.evaluation.dto.resdto.CalibrationLogResDto;
import com._team._team.evaluation.repository.CalibrationLogRepository;
import com._team._team.evaluation.repository.EvaluationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CalibrationLogService {

    final CalibrationLogRepository calibrationLogRepository;
    final EvaluationRepository evaluationRepository;

    @Autowired
    public CalibrationLogService(CalibrationLogRepository calibrationLogRepository,
                                 EvaluationRepository evaluationRepository) {
        this.calibrationLogRepository = calibrationLogRepository;
        this.evaluationRepository = evaluationRepository;
    }

    @Transactional
    public CalibrationLogResDto createCalibration(CalibrationCreateReqDto dto, UUID requesterId, UUID companyId) {
        Evaluation evaluation = evaluationRepository.findById(dto.getEvaluationId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가를 찾을 수 없습니다."));

        if (!evaluation.getEvaluationPolicy().getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "타사 평가에 대한 조정 내역을 기록할 수 없습니다.");
        }

        CalibrationLog log = CalibrationLog.builder()
                .evaluation(evaluation)
                .adjustedBy(requesterId)
                .beforeGrade(dto.getBeforeGrade())
                .afterGrade(dto.getAfterGrade())
                .beforeScore(dto.getBeforeScore())
                .afterScore(dto.getAfterScore())
                .reason(dto.getReason())
                .build();

        return CalibrationLogResDto.from(calibrationLogRepository.save(log));
    }

    @Transactional(readOnly = true)
    public List<CalibrationLogResDto> listByEvaluation(UUID evaluationId, UUID companyId) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가를 찾을 수 없습니다."));

        if (!evaluation.getEvaluationPolicy().getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "타사 평가의 조정 내역을 조회할 수 없습니다.");
        }

        return calibrationLogRepository.findByEvaluation_EvaluationIdOrderByAdjustedAtDesc(evaluationId)
                .stream()
                .map(CalibrationLogResDto::from)
                .collect(Collectors.toList());
    }
}
