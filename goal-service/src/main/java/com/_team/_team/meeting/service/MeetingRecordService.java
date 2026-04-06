package com._team._team.meeting.service;

import com._team._team.dto.BusinessException;
import com._team._team.meeting.domain.MeetingRecord;
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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MeetingRecordService {

    final MeetingRecordRepository meetingRecordRepository;

    @Autowired
    public MeetingRecordService(MeetingRecordRepository meetingRecordRepository) {
        this.meetingRecordRepository = meetingRecordRepository;
    }

    @Transactional
    public MeetingRecordResDto createMeeting(MeetingRecordCreateReqDto dto) {
        MeetingRecord parentRecord = null;
        if (dto.getParentRecordId() != null) {
            parentRecord = meetingRecordRepository.findById(dto.getParentRecordId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                            "상위 면담 기록을 찾을 수 없습니다. id=" + dto.getParentRecordId()));
        }

        MeetingRecord meetingRecord = MeetingRecord.builder()
                .parentRecord(parentRecord)
                .memberId(dto.getMemberId())
                .managerId(dto.getManagerId())
                .repeatCycle(dto.getRepeatCycle())
                .scheduledAt(dto.getScheduledAt())
                .agenda(dto.getAgenda())
                .build();

        MeetingRecord saved = meetingRecordRepository.save(meetingRecord);
        return MeetingRecordResDto.from(saved);
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

        return MeetingRecordResDto.from(meetingRecord);
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
        meetingRecord.complete(dto.getMemo(), dto.getManagerReaction());
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
