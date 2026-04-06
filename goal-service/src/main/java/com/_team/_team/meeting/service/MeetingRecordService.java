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
    public MeetingRecordResDto getMeeting(UUID meetingRecordId) {
        MeetingRecord meetingRecord = meetingRecordRepository.findById(meetingRecordId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "면담 기록을 찾을 수 없습니다. id=" + meetingRecordId));
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
    public MeetingRecordResDto completeMeeting(UUID meetingRecordId, MeetingRecordCompleteReqDto dto) {
        MeetingRecord meetingRecord = meetingRecordRepository.findById(meetingRecordId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "면담 기록을 찾을 수 없습니다. id=" + meetingRecordId));
        meetingRecord.complete(dto.getMemo(), dto.getManagerReaction());
        if (dto.getPrivateMemo() != null) {
            meetingRecord.updatePrivateMemo(dto.getPrivateMemo());
        }
        return MeetingRecordResDto.from(meetingRecord);
    }

    @Transactional
    public MeetingRecordResDto updateMeeting(UUID meetingRecordId, MeetingRecordUpdateReqDto dto) {
        MeetingRecord meetingRecord = meetingRecordRepository.findById(meetingRecordId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "면담 기록을 찾을 수 없습니다. id=" + meetingRecordId));
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
    public MeetingRecordResDto recordMemberReaction(UUID meetingRecordId, MemberReactionReqDto dto) {
        MeetingRecord meetingRecord = meetingRecordRepository.findById(meetingRecordId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "면담 기록을 찾을 수 없습니다. id=" + meetingRecordId));
        meetingRecord.recordMemberReaction(dto.getMemberReaction());
        return MeetingRecordResDto.from(meetingRecord);
    }

    @Transactional
    public MeetingRecordResDto updatePrivateMemo(UUID meetingRecordId, String privateMemo) {
        MeetingRecord meetingRecord = meetingRecordRepository.findById(meetingRecordId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "면담 기록을 찾을 수 없습니다. id=" + meetingRecordId));
        meetingRecord.updatePrivateMemo(privateMemo);
        return MeetingRecordResDto.from(meetingRecord);
    }
}
