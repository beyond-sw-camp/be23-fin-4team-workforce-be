package com._team._team.meeting.service;

import com._team._team.dto.BusinessException;
import com._team._team.meeting.domain.MeetingAction;
import com._team._team.meeting.domain.MeetingRecord;
import com._team._team.meeting.dto.reqdto.MeetingActionCreateReqDto;
import com._team._team.meeting.dto.reqdto.MeetingActionRateReqDto;
import com._team._team.meeting.dto.resdto.MeetingActionResDto;
import com._team._team.meeting.repository.MeetingActionRepository;
import com._team._team.meeting.repository.MeetingRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MeetingActionService {

    final MeetingActionRepository meetingActionRepository;
    final MeetingRecordRepository meetingRecordRepository;

    @Autowired
    public MeetingActionService(MeetingActionRepository meetingActionRepository,
                                MeetingRecordRepository meetingRecordRepository) {
        this.meetingActionRepository = meetingActionRepository;
        this.meetingRecordRepository = meetingRecordRepository;
    }

    @Transactional
    public MeetingActionResDto createAction(UUID meetingRecordId, MeetingActionCreateReqDto dto) {
        MeetingRecord meetingRecord = meetingRecordRepository.findById(meetingRecordId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "면담 기록을 찾을 수 없습니다. id=" + meetingRecordId));

        MeetingAction meetingAction = MeetingAction.builder()
                .meetingRecord(meetingRecord)
                .assigneeId(dto.getAssigneeId())
                .description(dto.getDescription())
                .dueDate(dto.getDueDate())
                .isCompleted(false)
                .build();

        MeetingAction saved = meetingActionRepository.save(meetingAction);
        return MeetingActionResDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<MeetingActionResDto> listActions(UUID meetingRecordId) {
        return meetingActionRepository.findByMeetingRecord_MeetingRecordIdOrderByCreatedAtAsc(meetingRecordId)
                .stream()
                .map(MeetingActionResDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MeetingActionResDto> listPendingByAssignee(UUID assigneeId) {
        return meetingActionRepository.findByAssigneeIdAndIsCompletedFalse(assigneeId)
                .stream()
                .map(MeetingActionResDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public MeetingActionResDto completeAction(UUID meetingActionId) {
        MeetingAction meetingAction = meetingActionRepository.findById(meetingActionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "액션 아이템을 찾을 수 없습니다. id=" + meetingActionId));
        meetingAction.complete();
        return MeetingActionResDto.from(meetingAction);
    }

    @Transactional
    public MeetingActionResDto rateAction(UUID meetingActionId, MeetingActionRateReqDto dto) {
        MeetingAction meetingAction = meetingActionRepository.findById(meetingActionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "액션 아이템을 찾을 수 없습니다. id=" + meetingActionId));
        meetingAction.rate(dto.getTlRating());
        return MeetingActionResDto.from(meetingAction);
    }

    @Transactional
    public MeetingActionResDto linkApproval(UUID meetingActionId, UUID approvalId) {
        MeetingAction meetingAction = meetingActionRepository.findById(meetingActionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "액션 아이템을 찾을 수 없습니다. id=" + meetingActionId));
        meetingAction.linkApproval(approvalId);
        return MeetingActionResDto.from(meetingAction);
    }

    @Transactional
    public void deleteAction(UUID meetingActionId) {
        MeetingAction meetingAction = meetingActionRepository.findById(meetingActionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "액션 아이템을 찾을 수 없습니다. id=" + meetingActionId));
        meetingActionRepository.delete(meetingAction);
    }
}
