package com._team._team.evaluation.service;

import com._team._team.dto.BusinessException;
import com._team._team.evaluation.domain.CalibrationHistory;
import com._team._team.evaluation.domain.EvaluationResponse;
import com._team._team.evaluation.domain.EvaluationSeason;
import com._team._team.evaluation.domain.enums.EvaluationStatus;
import com._team._team.evaluation.domain.enums.SeasonPhase;
import com._team._team.evaluation.domain.enums.SeasonStatus;
import com._team._team.evaluation.dto.reqdto.CalibrationAdjustReqDto;
import com._team._team.evaluation.dto.reqdto.ResponseSaveReqDto;
import com._team._team.evaluation.dto.resdto.ResponseResDto;
import com._team._team.evaluation.repository.CalibrationHistoryRepository;
import com._team._team.evaluation.repository.EvaluationResponseRepository;
import com._team._team.evaluation.repository.EvaluationSeasonRepository;
import com._team._team.meeting.service.MeetingRecordService;
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
    private final EvaluationSeasonRepository seasonRepository;
    private final CalibrationHistoryRepository calibrationHistoryRepository;
    private final ObjectMapper objectMapper;
    private final MeetingRecordService meetingRecordService;

    public EvaluationResponseService(EvaluationResponseRepository responseRepository,
                                      EvaluationSeasonRepository seasonRepository,
                                      CalibrationHistoryRepository calibrationHistoryRepository,
                                      ObjectMapper objectMapper,
                                      MeetingRecordService meetingRecordService) {
        this.responseRepository = responseRepository;
        this.seasonRepository = seasonRepository;
        this.calibrationHistoryRepository = calibrationHistoryRepository;
        this.objectMapper = objectMapper;
        this.meetingRecordService = meetingRecordService;
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
        // 현재 시즌 phase 가 응답 편집 가능한 단계가 아니면 차단
        EvaluationSeason season = response.getGroup().getSeason();
        if (season.getStatus() != SeasonStatus.ACTIVE || !season.isResponseEditablePhase()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "현재 평가 페이즈에서는 응답을 저장할 수 없습니다. phase=" + season.getPhase());
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
        // 현재 phase 가 편집 가능한 단계가 아니면 제출 차단
        EvaluationSeason season = response.getGroup().getSeason();
        if (season.getStatus() != SeasonStatus.ACTIVE || !season.isResponseEditablePhase()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "현재 평가 페이즈에서는 제출할 수 없습니다. phase=" + season.getPhase());
        }
        if (response.getAnswersJson() == null || response.getAnswersJson().isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "응답 내용이 비어있어 제출할 수 없습니다.");
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
        // 캘리브레이션이 확정된 뒤에는 재개방 불가
        EvaluationSeason season = response.getGroup().getSeason();
        if (season.getPhase() == SeasonPhase.CONFIRMED || season.getPhase() == SeasonPhase.PUBLISHED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "캘리브레이션 확정/결과 공개 이후에는 응답을 재개방할 수 없습니다.");
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
    }

    // 캘리브레이션
    @Transactional
    public void adjustCalibrations(UUID seasonId, UUID companyId, UUID actorMemberId, CalibrationAdjustReqDto dto) {
        EvaluationSeason season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "시즌을 찾을 수 없습니다."));
        if (!season.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "시즌을 찾을 수 없습니다.");
        }
        // 캘리브레이션 단계에서만 조정 가능
        if (!season.isCalibrationPhase()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "캘리브레이션 페이즈에서만 조정이 가능합니다. 현재 phase=" + season.getPhase());
        }
        for (CalibrationAdjustReqDto.Adjustment adj : dto.getAdjustments()) {
            EvaluationResponse response = responseRepository.findById(adj.getResponseId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가 응답을 찾을 수 없습니다."));
            if (!response.getCompanyId().equals(companyId)) {
                throw new BusinessException(HttpStatus.NOT_FOUND, "평가 응답을 찾을 수 없습니다.");
            }
            if (response.getStatus() != EvaluationStatus.SUBMITTED) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "제출되지 않은 응답은 캘리브레이션할 수 없습니다. responseId=" + response.getResponseId());
            }
            String previousGrade = extractAdjustedGrade(response.getCalibrationJson());
            try {
                ObjectNode calibration = objectMapper.createObjectNode();
                if (response.getCalibrationJson() != null) {
                    calibration = (ObjectNode) objectMapper.readTree(response.getCalibrationJson());
                }
                calibration.put("adjustedGrade", adj.getAdjustedGrade());
                calibration.put("adjustmentReason", adj.getAdjustmentReason());
                calibration.put("adjustedAt", LocalDateTime.now().toString());
                calibration.put("adjustedBy", actorMemberId != null ? actorMemberId.toString() : "unknown");
                response.updateCalibration(objectMapper.writeValueAsString(calibration));
            } catch (Exception e) {
                throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "캘리브레이션 데이터 처리 실패");
            }
            // 감사 이력 저장 — 매 조정마다 append
            calibrationHistoryRepository.save(CalibrationHistory.builder()
                    .companyId(companyId)
                    .seasonId(seasonId)
                    .responseId(response.getResponseId())
                    .adjustedGrade(adj.getAdjustedGrade())
                    .adjustmentReason(adj.getAdjustmentReason())
                    .previousGrade(previousGrade)
                    .adjustedBy(actorMemberId != null ? actorMemberId : companyId)
                    .build());
        }
    }

    @Transactional
    public void confirmCalibration(UUID seasonId, UUID companyId) {
        EvaluationSeason season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "시즌을 찾을 수 없습니다."));
        if (!season.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "시즌을 찾을 수 없습니다.");
        }
        // 캘리브레이션 페이즈에서만 확정 가능
        if (!season.isCalibrationPhase()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "캘리브레이션 페이즈에서만 확정할 수 있습니다. 현재 phase=" + season.getPhase());
        }
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
        // Phase 전이: CALIBRATION → CONFIRMED
        season.transitionTo(SeasonPhase.CONFIRMED);
    }

    /**
     * 결과 공개 — CONFIRMED → PUBLISHED 전이 후 피드백 면담을 바로 생성
     */
    @Transactional
    public void publishResults(UUID seasonId, UUID companyId) {
        EvaluationSeason season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "시즌을 찾을 수 없습니다."));
        if (!season.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "시즌을 찾을 수 없습니다.");
        }
        if (season.getPhase() != SeasonPhase.CONFIRMED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "확정(CONFIRMED) 이후에만 결과를 공개할 수 있습니다. 현재 phase=" + season.getPhase());
        }
        season.transitionTo(SeasonPhase.PUBLISHED);
        // 피드백 면담 자동 생성 (같은 트랜잭션에서 수행)
        meetingRecordService.createFeedbackMeetingsForSeason(seasonId, companyId);
    }

    private String extractAdjustedGrade(String calibrationJson) {
        if (calibrationJson == null) return null;
        try {
            var node = objectMapper.readTree(calibrationJson);
            if (node.has("adjustedGrade")) {
                return node.get("adjustedGrade").asText();
            }
        } catch (Exception ignore) {
        }
        return null;
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