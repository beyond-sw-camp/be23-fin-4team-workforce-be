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
            @RequestHeader("X-User-UUID") String requesterId,
            @RequestBody @Valid MeetingRecordCreateReqDto dto) {
        return ApiResponse.success(meetingRecordService.createMeeting(dto), "면담 기록이 생성되었습니다.");
    }

    // 면담 기록 상세 조회
    @CheckPermission(resource = Resource.MEETING, action = Action.READ)
    @GetMapping("/{meetingRecordId}")
    public ApiResponse<MeetingRecordResDto> getMeeting(
            @RequestHeader("X-User-UUID") String requesterId,
            @PathVariable UUID meetingRecordId) {
        return ApiResponse.success(meetingRecordService.getMeeting(meetingRecordId, UUID.fromString(requesterId)), "면담 기록 조회 성공");
    }

    // 나의 면담 기록 목록 조회 (멤버로서)
    @CheckPermission(resource = Resource.MEETING, action = Action.READ)
    @GetMapping("/me/as-member")
    public ApiResponse<List<MeetingRecordResDto>> listMyMeetingsAsMember(
            @RequestHeader("X-User-UUID") String memberId) {
        return ApiResponse.success(meetingRecordService.listByMember(UUID.fromString(memberId)), "나의 면담 기록 목록 조회 성공");
    }

    // 나의 면담 기록 목록 조회 (매니저로서)
    @CheckPermission(resource = Resource.MEETING, action = Action.READ)
    @GetMapping("/me/as-manager")
    public ApiResponse<List<MeetingRecordResDto>> listMyMeetingsAsManager(
            @RequestHeader("X-User-UUID") String managerId) {
        return ApiResponse.success(meetingRecordService.listByManager(UUID.fromString(managerId)), "매니저별 면담 기록 목록 조회 성공");
    }

    // 특정 멤버와의 면담 기록 목록 조회 (매니저 권한 등 확인 필요)
    @CheckPermission(resource = Resource.MEETING, action = Action.READ)
    @GetMapping("/member/{memberId}/manager/me")
    public ApiResponse<List<MeetingRecordResDto>> listWithMember(
            @PathVariable UUID memberId,
            @RequestHeader("X-User-UUID") String managerId) {
        return ApiResponse.success(meetingRecordService.listByMemberAndManager(memberId, UUID.fromString(managerId)), "멤버-매니저 면담 기록 목록 조회 성공");
    }

    // 면담 완료 처리
    @CheckPermission(resource = Resource.MEETING, action = Action.UPDATE)
    @PatchMapping("/{meetingRecordId}/complete")
    public ApiResponse<MeetingRecordResDto> completeMeeting(
            @RequestHeader("X-User-UUID") String requesterId,
            @PathVariable UUID meetingRecordId,
            @RequestBody MeetingRecordCompleteReqDto dto) {
        return ApiResponse.success(meetingRecordService.completeMeeting(meetingRecordId, UUID.fromString(requesterId), dto), "면담이 완료되었습니다.");
    }

    // 면담 기록 수정
    @CheckPermission(resource = Resource.MEETING, action = Action.UPDATE)
    @PatchMapping("/{meetingRecordId}")
    public ApiResponse<MeetingRecordResDto> updateMeeting(
            @RequestHeader("X-User-UUID") String requesterId,
            @PathVariable UUID meetingRecordId,
            @RequestBody MeetingRecordUpdateReqDto dto) {
        return ApiResponse.success(meetingRecordService.updateMeeting(meetingRecordId, UUID.fromString(requesterId), dto), "면담 기록이 수정되었습니다.");
    }

    // 멤버 반응 기록
    @CheckPermission(resource = Resource.MEETING, action = Action.UPDATE)
    @PatchMapping("/{meetingRecordId}/member-reaction")
    public ApiResponse<MeetingRecordResDto> recordMemberReaction(
            @RequestHeader("X-User-UUID") String requesterId,
            @PathVariable UUID meetingRecordId,
            @RequestBody @Valid MemberReactionReqDto dto) {
        return ApiResponse.success(meetingRecordService.recordMemberReaction(meetingRecordId, UUID.fromString(requesterId), dto), "멤버 반응이 기록되었습니다.");
    }

    // 비공개 메모 수정
    @CheckPermission(resource = Resource.MEETING, action = Action.UPDATE)
    @PatchMapping("/{meetingRecordId}/private-memo")
    public ApiResponse<MeetingRecordResDto> updatePrivateMemo(
            @RequestHeader("X-User-UUID") String requesterId,
            @PathVariable UUID meetingRecordId,
            @RequestBody PrivateMemoUpdateReqDto dto) {
        return ApiResponse.success(meetingRecordService.updatePrivateMemo(meetingRecordId, UUID.fromString(requesterId), dto.getPrivateMemo()), "비공개 메모가 수정되었습니다.");
    }
}
