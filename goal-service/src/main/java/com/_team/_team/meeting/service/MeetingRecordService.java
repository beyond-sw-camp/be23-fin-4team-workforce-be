package com._team._team.meeting.service;

import com._team._team.dto.BusinessException;
import com._team._team.evaluation.domain.EvaluationResponse;
import com._team._team.evaluation.domain.enums.EvalType;
import com._team._team.evaluation.repository.EvaluationResponseRepository;
import com._team._team.meeting.domain.MeetingRecord;
import com._team._team.meeting.domain.enums.RepeatCycle;
import com._team._team.meeting.dto.reqdto.MeetingRecordCompleteReqDto;
import com._team._team.meeting.dto.reqdto.MeetingRecordCreateReqDto;
import com._team._team.meeting.dto.reqdto.MeetingRecordUpdateReqDto;
import com._team._team.meeting.dto.reqdto.MemberReactionReqDto;
import com._team._team.meeting.dto.resdto.MeetingRecordResDto;
import com._team._team.meeting.repository.MeetingRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MeetingRecordService {

    final MeetingRecordRepository meetingRecordRepository;
    final EvaluationResponseRepository evaluationResponseRepository;

    @Autowired
    public MeetingRecordService(MeetingRecordRepository meetingRecordRepository,
                                EvaluationResponseRepository evaluationResponseRepository) {
        this.meetingRecordRepository = meetingRecordRepository;
        this.evaluationResponseRepository = evaluationResponseRepository;
    }

    /**
     * 평가 결과가 공개(PUBLISHED)된 직후, 해당 시즌의 DOWNWARD 응답을 순회하면서
     * 대상 멤버별로 "평가 결과 피드백 면담" 을 한 건씩 자동 생성합니다.
     *
     * <p>원칙:</p>
     * <ul>
     *     <li>대상 직원 1명에 대해 한 시즌에 피드백 면담 1건만 생성.</li>
     *     <li>매니저는 DOWNWARD 평가의 평가자(evaluatorId) 로 추정. DOWNWARD 응답이 없으면
     *         멤버의 직속 매니저를 알 수 없으므로 스킵하고 관리자가 수동 생성하도록 안내.</li>
     *     <li>예약 시점은 호출 시점 + 3일 로 임시 설정 — 관리자가 이후 수정 가능.</li>
     * </ul>
     */
    @Transactional
    public void createFeedbackMeetingsForSeason(UUID seasonId, UUID companyId) {
        // 멱등성: 이미 해당 시즌에 피드백 면담이 생성되어 있으면 중복 생성하지 않음
        if (meetingRecordRepository.existsByRelatedSeasonIdAndCompanyId(seasonId, companyId)) {
            return;
        }

        Set<UUID> alreadyScheduled = new HashSet<>();

        List<EvaluationResponse> responses = evaluationResponseRepository.findByGroup_Season_SeasonId(seasonId);
        for (EvaluationResponse response : responses) {
            if (!response.getCompanyId().equals(companyId)) continue;
            if (response.getEvaluationType() != EvalType.DOWNWARD) continue;

            UUID memberId = response.getTargetMemberId();
            UUID managerId = response.getEvaluatorId();
            if (memberId == null || managerId == null) continue;
            if (!alreadyScheduled.add(memberId)) continue;

            MeetingRecord feedback = MeetingRecord.builder()
                    .memberId(memberId)
                    .managerId(managerId)
                    .repeatCycle(RepeatCycle.ONE_TIME)
                    .scheduledAt(LocalDateTime.now().plusDays(3))
                    .agenda("평가 결과 피드백 면담 — 시즌: " + seasonId)
                    .relatedEvaluationResponseId(response.getResponseId())
                    .relatedSeasonId(seasonId)
                    .companyId(companyId)
                    .build();
            // [D-5] targetGoalIdsJson 제거됨. goalSnapshotJson 에서 goalId 목록만 추출해 링크.
            String snap = response.getGoalSnapshotJson();
            if (snap != null && !snap.isBlank()) {
                try {
                    com.fasterxml.jackson.databind.JsonNode arr =
                            new com.fasterxml.jackson.databind.ObjectMapper().readTree(snap);
                    if (arr.isArray() && arr.size() > 0) {
                        List<String> ids = new java.util.ArrayList<>();
                        for (com.fasterxml.jackson.databind.JsonNode n : arr) {
                            com.fasterxml.jackson.databind.JsonNode idNode = n.get("goalId");
                            if (idNode != null && !idNode.isNull()) ids.add(idNode.asText());
                        }
                        if (!ids.isEmpty()) {
                            feedback.linkGoals(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(ids));
                        }
                    }
                } catch (Exception ignore) {
                    // 스냅샷 파싱 실패해도 미팅 생성 자체는 진행
                }
            }
            meetingRecordRepository.save(feedback);
        }
    }

    @Transactional
    public MeetingRecordResDto createMeeting(MeetingRecordCreateReqDto dto, UUID companyId) {
        // [M3] memberId == managerId 인 셀프 미팅은 비정상 케이스로 차단
        if (dto.getMemberId() != null && dto.getMemberId().equals(dto.getManagerId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "본인 자신을 매니저로 둔 1:1 미팅은 생성할 수 없습니다.");
        }
        MeetingRecord parentRecord = null;
        if (dto.getParentRecordId() != null) {
            parentRecord = meetingRecordRepository.findById(dto.getParentRecordId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                            "상위 면담 기록을 찾을 수 없습니다. id=" + dto.getParentRecordId()));
            // 부모 미팅과 memberId/managerId 조합이 달라지면 의미상 혼동되므로 차단
            if (!parentRecord.getMemberId().equals(dto.getMemberId())
                    || !parentRecord.getManagerId().equals(dto.getManagerId())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "상위 면담과 동일한 멤버/매니저 조합이어야 합니다.");
            }
        }

        MeetingRecord meetingRecord = MeetingRecord.builder()
                .parentRecord(parentRecord)
                .memberId(dto.getMemberId())
                .managerId(dto.getManagerId())
                .repeatCycle(dto.getRepeatCycle())
                .scheduledAt(dto.getScheduledAt())
                .agenda(dto.getAgenda())
                .companyId(companyId)
                .build();

        MeetingRecord saved = meetingRecordRepository.save(meetingRecord);
        return MeetingRecordResDto.from(saved);
    }

    /**
     * [M9] 미팅에서 논의될 연관 목표 ID 목록을 등록/수정합니다.
     */
    @Transactional
    public MeetingRecordResDto linkGoals(UUID meetingRecordId, UUID requesterId, String relatedGoalIdsJson) {
        MeetingRecord meetingRecord = meetingRecordRepository.findById(meetingRecordId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "면담 기록을 찾을 수 없습니다. id=" + meetingRecordId));
        if (!meetingRecord.getManagerId().equals(requesterId)
                && !meetingRecord.getMemberId().equals(requesterId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "해당 면담 기록의 당사자만 목표를 연결할 수 있습니다.");
        }
        meetingRecord.linkGoals(relatedGoalIdsJson);
        return MeetingRecordResDto.from(meetingRecord);
    }

    @Transactional(readOnly = true)
    public MeetingRecordResDto getMeeting(UUID meetingRecordId, UUID requesterId) {
        MeetingRecord meetingRecord = meetingRecordRepository.findById(meetingRecordId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "면담 기록을 찾을 수 없습니다. id=" + meetingRecordId));

        // 당사자 확인
        if (!meetingRecord.getMemberId().equals(requesterId) && !meetingRecord.getManagerId().equals(requesterId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "해당 면담 기록을 조회할 권한이 없습니다.");
        }

        MeetingRecordResDto dto = MeetingRecordResDto.from(meetingRecord);
        // privateMemo 는 매니저에게만 노출 — 멤버가 조회하면 null 처리
        if (!meetingRecord.getManagerId().equals(requesterId)) {
            dto.setPrivateMemo(null);
        }
        return dto;
    }

    @Transactional(readOnly = true)
    public List<MeetingRecordResDto> listByMember(UUID memberId) {
        return meetingRecordRepository.findByMemberIdOrderByScheduledAtDesc(memberId)
                .stream()
                .map(MeetingRecordResDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MeetingRecordResDto> listByManager(UUID managerId) {
        return meetingRecordRepository.findByManagerIdOrderByScheduledAtDesc(managerId)
                .stream()
                .map(MeetingRecordResDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MeetingRecordResDto> listByMemberAndManager(UUID memberId, UUID managerId) {
        return meetingRecordRepository.findByMemberIdAndManagerIdOrderByScheduledAtDesc(memberId, managerId)
                .stream()
                .map(MeetingRecordResDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public MeetingRecordResDto completeMeeting(UUID meetingRecordId, UUID requesterId, MeetingRecordCompleteReqDto dto) {
        MeetingRecord meetingRecord = meetingRecordRepository.findById(meetingRecordId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "면담 기록을 찾을 수 없습니다. id=" + meetingRecordId));

        // 매니저 확인
        if (!meetingRecord.getManagerId().equals(requesterId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "매니저만 면담을 완료 처리할 수 있습니다.");
        }

        if (meetingRecord.getCompletedAt() != null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "이미 완료된 면담 기록입니다.");
        }
        // [M1] 완료 시 memo 는 필수 — 회의록 없는 "완료" 는 회고/후속조치 추적을 막는다.
        if (dto.getMemo() == null || dto.getMemo().isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "면담 완료 시 메모(memo)는 필수입니다.");
        }
        try {
            meetingRecord.complete(dto.getMemo(), dto.getManagerReaction());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        if (dto.getPrivateMemo() != null) {
            meetingRecord.updatePrivateMemo(dto.getPrivateMemo());
        }
        return MeetingRecordResDto.from(meetingRecord);
    }

    @Transactional
    public MeetingRecordResDto updateMeeting(UUID meetingRecordId, UUID requesterId, MeetingRecordUpdateReqDto dto) {
        MeetingRecord meetingRecord = meetingRecordRepository.findById(meetingRecordId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "면담 기록을 찾을 수 없습니다. id=" + meetingRecordId));

        // 매니저 확인
        if (!meetingRecord.getManagerId().equals(requesterId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "매니저만 면담 일정을 수정할 수 있습니다.");
        }

        if (dto.getScheduledAt() != null) {
            meetingRecord.updateScheduledAt(dto.getScheduledAt());
        }
        if (dto.getAgenda() != null) {
            meetingRecord.updateAgenda(dto.getAgenda());
        }
        if (dto.getRepeatCycle() != null) {
            meetingRecord.updateRepeatCycle(dto.getRepeatCycle());
        }
        return MeetingRecordResDto.from(meetingRecord);
    }

    @Transactional
    public MeetingRecordResDto recordMemberReaction(UUID meetingRecordId, UUID requesterId, MemberReactionReqDto dto) {
        MeetingRecord meetingRecord = meetingRecordRepository.findById(meetingRecordId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "면담 기록을 찾을 수 없습니다. id=" + meetingRecordId));

        // 멤버 확인
        if (!meetingRecord.getMemberId().equals(requesterId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "본인의 면담에 대해서만 반응을 기록할 수 있습니다.");
        }

        meetingRecord.recordMemberReaction(dto.getMemberReaction());
        return MeetingRecordResDto.from(meetingRecord);
    }

    @Transactional
    public MeetingRecordResDto updatePrivateMemo(UUID meetingRecordId, UUID requesterId, String privateMemo) {
        MeetingRecord meetingRecord = meetingRecordRepository.findById(meetingRecordId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "면담 기록을 찾을 수 없습니다. id=" + meetingRecordId));

        // 매니저 확인
        if (!meetingRecord.getManagerId().equals(requesterId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "매니저만 비공개 메모를 작성할 수 있습니다.");
        }

        meetingRecord.updatePrivateMemo(privateMemo);
        return MeetingRecordResDto.from(meetingRecord);
    }
}
