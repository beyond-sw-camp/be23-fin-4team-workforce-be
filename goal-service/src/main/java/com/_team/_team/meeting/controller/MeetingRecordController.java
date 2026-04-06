package com._team._team.meeting.controller;

import com._team._team.annotation.Action;
import com._team._team.annotation.CheckPermission;
import com._team._team.annotation.Resource;
import com._team._team.dto.ApiResponse;
import com._team._team.meeting.dto.reqdto.MeetingRecordCompleteReqDto;
import com._team._team.meeting.dto.reqdto.MeetingRecordCreateReqDto;
import com._team._team.meeting.dto.reqdto.MeetingRecordUpdateReqDto;
import com._team._team.meeting.dto.reqdto.MemberReactionReqDto;
import com._team._team.meeting.dto.reqdto.PrivateMemoUpdateReqDto;
import com._team._team.meeting.dto.resdto.MeetingRecordResDto;
import com._team._team.meeting.service.MeetingRecordService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// 면담 기록 생성, 조회, 완료, 수정, 반응 및 비공개 메모 관리 컨트롤러
@RestController
@RequestMapping("/meeting/record")
public class MeetingRecordController {

    final MeetingRecordService meetingRecordService;

    @Autowired
    public MeetingRecordController(MeetingRecordService meetingRecordService) {
        this.meetingRecordService = meetingRecordService;
    }

    // 면담 기록 생성
    @CheckPermission(resource = Resource.MEETING, action = Action.CREATE)
    @PostMapping
    public ApiResponse<MeetingRecordResDto> createMeeting(
            @RequestBody @Valid MeetingRecordCreateReqDto dto) {
        return ApiResponse.success(meetingRecordService.createMeeting(dto), "면담 기록이 생성되었습니다.");
    }

    // 면담 기록 상세 조회
    @CheckPermission(resource = Resource.MEETING, action = Action.READ)
    @GetMapping("/{meetingRecordId}")
    public ApiResponse<MeetingRecordResDto> getMeeting(
            @PathVariable UUID meetingRecordId) {
        return ApiResponse.success(meetingRecordService.getMeeting(meetingRecordId), "면담 기록 조회 성공");
    }

    // 멤버별 면담 기록 목록 조회
    @CheckPermission(resource = Resource.MEETING, action = Action.READ)
    @GetMapping("/member/{memberId}")
    public ApiResponse<List<MeetingRecordResDto>> listByMember(
            @PathVariable UUID memberId) {
        return ApiResponse.success(meetingRecordService.listByMember(memberId), "멤버별 면담 기록 목록 조회 성공");
    }

    // 매니저별 면담 기록 목록 조회
    @CheckPermission(resource = Resource.MEETING, action = Action.READ)
    @GetMapping("/manager/{managerId}")
    public ApiResponse<List<MeetingRecordResDto>> listByManager(
            @PathVariable UUID managerId) {
        return ApiResponse.success(meetingRecordService.listByManager(managerId), "매니저별 면담 기록 목록 조회 성공");
    }

    // 멤버-매니저 조합 면담 기록 목록 조회
    @CheckPermission(resource = Resource.MEETING, action = Action.READ)
    @GetMapping("/member/{memberId}/manager/{managerId}")
    public ApiResponse<List<MeetingRecordResDto>> listByMemberAndManager(
            @PathVariable UUID memberId,
            @PathVariable UUID managerId) {
        return ApiResponse.success(meetingRecordService.listByMemberAndManager(memberId, managerId), "멤버-매니저 면담 기록 목록 조회 성공");
    }

    // 면담 완료 처리
    @CheckPermission(resource = Resource.MEETING, action = Action.UPDATE)
    @PatchMapping("/{meetingRecordId}/complete")
    public ApiResponse<MeetingRecordResDto> completeMeeting(
            @PathVariable UUID meetingRecordId,
            @RequestBody MeetingRecordCompleteReqDto dto) {
        return ApiResponse.success(meetingRecordService.completeMeeting(meetingRecordId, dto), "면담이 완료되었습니다.");
    }

    // 면담 기록 수정
    @CheckPermission(resource = Resource.MEETING, action = Action.UPDATE)
    @PatchMapping("/{meetingRecordId}")
    public ApiResponse<MeetingRecordResDto> updateMeeting(
            @PathVariable UUID meetingRecordId,
            @RequestBody MeetingRecordUpdateReqDto dto) {
        return ApiResponse.success(meetingRecordService.updateMeeting(meetingRecordId, dto), "면담 기록이 수정되었습니다.");
    }

    // 멤버 반응 기록
    @CheckPermission(resource = Resource.MEETING, action = Action.UPDATE)
    @PatchMapping("/{meetingRecordId}/member-reaction")
    public ApiResponse<MeetingRecordResDto> recordMemberReaction(
            @PathVariable UUID meetingRecordId,
            @RequestBody @Valid MemberReactionReqDto dto) {
        return ApiResponse.success(meetingRecordService.recordMemberReaction(meetingRecordId, dto), "멤버 반응이 기록되었습니다.");
    }

    // 비공개 메모 수정
    @CheckPermission(resource = Resource.MEETING, action = Action.UPDATE)
    @PatchMapping("/{meetingRecordId}/private-memo")
    public ApiResponse<MeetingRecordResDto> updatePrivateMemo(
            @PathVariable UUID meetingRecordId,
            @RequestBody PrivateMemoUpdateReqDto dto) {
        return ApiResponse.success(meetingRecordService.updatePrivateMemo(meetingRecordId, dto.getPrivateMemo()), "비공개 메모가 수정되었습니다.");
    }
}
