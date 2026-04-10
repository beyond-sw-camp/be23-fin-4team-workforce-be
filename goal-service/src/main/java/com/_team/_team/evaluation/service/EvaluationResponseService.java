package com._team._team.evaluation.service;

import com._team._team.dto.BusinessException;
import com._team._team.evaluation.domain.EvaluationResponse;
import com._team._team.evaluation.domain.enums.EvaluationStatus;
import com._team._team.evaluation.dto.reqdto.CalibrationAdjustReqDto;
import com._team._team.evaluation.dto.reqdto.ResponseSaveReqDto;
import com._team._team.evaluation.dto.resdto.ResponseResDto;
import com._team._team.evaluation.repository.EvaluationResponseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EvaluationResponseService {

    private final EvaluationResponseRepository responseRepository;
    private final ObjectMapper objectMapper;

    public EvaluationResponseService(EvaluationResponseRepository responseRepository,
                                      ObjectMapper objectMapper) {
        this.responseRepository = responseRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<ResponseResDto> listMyResponses(UUID evaluatorId) {
        return responseRepository.findByEvaluatorId(evaluatorId)
                .stream().map(ResponseResDto::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ResponseResDto getResponse(UUID responseId, UUID requesterId) {
        EvaluationResponse response = responseRepository.findById(responseId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가 응답을 찾을 수 없습니다."));
        // 평가자 본인 또는 관리자만 조회 가능 (관리자 체크는 컨트롤러/AOP에서 처리)
        return ResponseResDto.from(response);
    }

    @Transactional
    public ResponseResDto saveResponse(UUID responseId, UUID requesterId, ResponseSaveReqDto dto) {
        EvaluationResponse response = responseRepository.findById(responseId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가 응답을 찾을 수 없습니다."));
        if (!response.getEvaluatorId().equals(requesterId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "자신의 평가만 수정할 수 있습니다.");
        }
        if (response.getStatus() == EvaluationStatus.SUBMITTED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "이미 제출된 평가는 수정할 수 없습니다.");
        }
        response.saveAnswers(dto.getAnswersJson());
        return ResponseResDto.from(response);
    }

    @Transactional
    public ResponseResDto submitResponse(UUID responseId, UUID requesterId) {
        EvaluationResponse response = responseRepository.findById(responseId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가 응답을 찾을 수 없습니다."));
        if (!response.getEvaluatorId().equals(requesterId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "자신의 평가만 제출할 수 있습니다.");
        }
        if (response.getStatus() == EvaluationStatus.SUBMITTED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "이미 제출된 평가입니다.");
        }
        response.submit();
        return ResponseResDto.from(response);
    }

    @Transactional
    public ResponseResDto reopenResponse(UUID responseId, UUID companyId) {
        EvaluationResponse response = responseRepository.findById(responseId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가 응답을 찾을 수 없습니다."));
        if (!response.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "평가 응답을 찾을 수 없습니다.");
        }
        response.reopen();
        return ResponseResDto.from(response);
    }

    // 리마인드
    @Transactional
    public void sendReminder(UUID seasonId, UUID memberId, UUID companyId) {
        List<EvaluationResponse> responses = responseRepository.findByEvaluatorId(memberId)
                .stream()
                .filter(r -> r.getCompanyId().equals(companyId))
                .filter(r -> r.getStatus() != EvaluationStatus.SUBMITTED)
                .collect(Collectors.toList());
        for (EvaluationResponse r : responses) {
            r.updateRemindedAt();
        }
        // TODO: 인앱 알림 발송 연동
    }

    @Transactional
    public void sendBulkReminder(UUID seasonId, UUID companyId) {
        List<EvaluationResponse> responses = responseRepository.findByGroup_Season_SeasonId(seasonId)
                .stream()
                .filter(r -> r.getCompanyId().equals(companyId))
                .filter(r -> r.getStatus() != EvaluationStatus.SUBMITTED)
                .collect(Collectors.toList());
        for (EvaluationResponse r : responses) {
            r.updateRemindedAt();
        }
        // TODO: 인앱 알림 일괄 발송 연동
    }

    // 캘리브레이션
    @Transactional
    public void adjustCalibrations(UUID seasonId, UUID companyId, CalibrationAdjustReqDto dto) {
        for (CalibrationAdjustReqDto.Adjustment adj : dto.getAdjustments()) {
            EvaluationResponse response = responseRepository.findById(adj.getResponseId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가 응답을 찾을 수 없습니다."));
            if (!response.getCompanyId().equals(companyId)) {
                throw new BusinessException(HttpStatus.NOT_FOUND, "평가 응답을 찾을 수 없습니다.");
            }
            try {
                ObjectNode calibration = objectMapper.createObjectNode();
                // 기존 calibration 데이터가 있으면 병합
                if (response.getCalibrationJson() != null) {
                    calibration = (ObjectNode) objectMapper.readTree(response.getCalibrationJson());
                }
                calibration.put("adjustedGrade", adj.getAdjustedGrade());
                calibration.put("adjustmentReason", adj.getAdjustmentReason());
                response.updateCalibration(objectMapper.writeValueAsString(calibration));
            } catch (Exception e) {
                throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "캘리브레이션 데이터 처리 실패");
            }
        }
    }

    @Transactional
    public void confirmCalibration(UUID seasonId, UUID companyId) {
        List<EvaluationResponse> responses = responseRepository.findByGroup_Season_SeasonId(seasonId)
                .stream()
                .filter(r -> r.getCompanyId().equals(companyId))
                .collect(Collectors.toList());
        for (EvaluationResponse r : responses) {
            try {
                ObjectNode calibration;
                if (r.getCalibrationJson() != null) {
                    calibration = (ObjectNode) objectMapper.readTree(r.getCalibrationJson());
                } else {
                    calibration = objectMapper.createObjectNode();
                }
                calibration.put("confirmedAt", LocalDateTime.now().toString());
                r.updateCalibration(objectMapper.writeValueAsString(calibration));
            } catch (Exception e) {
                throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "캘리브레이션 확정 처리 실패");
            }
        }
    }

    // 진행도
    @Transactional(readOnly = true)
    public List<ResponseResDto> listBySeasonForProgress(UUID seasonId, UUID companyId) {
        return responseRepository.findByGroup_Season_SeasonId(seasonId)
                .stream()
                .filter(r -> r.getCompanyId().equals(companyId))
                .map(ResponseResDto::from)
                .collect(Collectors.toList());
    }

    // 오류 감지 (비동기 트리거 후 조회)
    @Transactional(readOnly = true)
    public List<ResponseResDto> listAnomalies(UUID seasonId, UUID companyId) {
        return responseRepository.findByGroup_Season_SeasonId(seasonId)
                .stream()
                .filter(r -> r.getCompanyId().equals(companyId))
                .filter(r -> {
                    if (r.getAnswersJson() == null) return false;
                    return r.getAnswersJson().contains("\"flagged\":true");
                })
                .map(ResponseResDto::from)
                .collect(Collectors.toList());
    }
}
