package com._team._team.meeting.controller;

import com._team._team.annotation.Action;
import com._team._team.annotation.CheckPermission;
import com._team._team.annotation.Resource;
import com._team._team.dto.ApiResponse;
import com._team._team.meeting.dto.reqdto.LinkApprovalReqDto;
import com._team._team.meeting.dto.reqdto.MeetingActionCreateReqDto;
import com._team._team.meeting.dto.reqdto.MeetingActionRateReqDto;
import com._team._team.meeting.dto.resdto.MeetingActionResDto;
import com._team._team.meeting.service.MeetingActionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// 면담 액션 아이템 생성, 조회, 완료, 평가, 결재 연결, 삭제 컨트롤러
@RestController
@RequestMapping("/meeting")
public class MeetingActionController {

    final MeetingActionService meetingActionService;

    @Autowired
    public MeetingActionController(MeetingActionService meetingActionService) {
        this.meetingActionService = meetingActionService;
    }

    // 액션 아이템 생성
    @CheckPermission(resource = Resource.GOAL, action = Action.CREATE)
    @PostMapping("/{meetingRecordId}/action")
    public ApiResponse<MeetingActionResDto> createAction(
            @PathVariable UUID meetingRecordId,
            @RequestBody @Valid MeetingActionCreateReqDto dto) {
        return ApiResponse.success(meetingActionService.createAction(meetingRecordId, dto), "액션 아이템이 생성되었습니다.");
    }

    // 액션 아이템 목록 조회
    @CheckPermission(resource = Resource.GOAL, action = Action.READ)
    @GetMapping("/{meetingRecordId}/action")
    public ApiResponse<List<MeetingActionResDto>> listActions(
            @PathVariable UUID meetingRecordId) {
        return ApiResponse.success(meetingActionService.listActions(meetingRecordId), "액션 아이템 목록 조회 성공");
    }

    // 담당자별 미완료 액션 아이템 조회
    @CheckPermission(resource = Resource.GOAL, action = Action.READ)
    @GetMapping("/action/assignee/{assigneeId}/pending")
    public ApiResponse<List<MeetingActionResDto>> listPendingByAssignee(
            @PathVariable UUID assigneeId) {
        return ApiResponse.success(meetingActionService.listPendingByAssignee(assigneeId), "미완료 액션 아이템 목록 조회 성공");
    }

    // 액션 아이템 완료 처리
    @CheckPermission(resource = Resource.GOAL, action = Action.UPDATE)
    @PatchMapping("/{meetingRecordId}/action/{meetingActionId}/complete")
    public ApiResponse<MeetingActionResDto> completeAction(
            @PathVariable UUID meetingRecordId,
            @PathVariable UUID meetingActionId) {
        return ApiResponse.success(meetingActionService.completeAction(meetingActionId), "액션 아이템이 완료되었습니다.");
    }

    // 액션 아이템 평가
    @CheckPermission(resource = Resource.GOAL, action = Action.UPDATE)
    @PatchMapping("/{meetingRecordId}/action/{meetingActionId}/rate")
    public ApiResponse<MeetingActionResDto> rateAction(
            @PathVariable UUID meetingRecordId,
            @PathVariable UUID meetingActionId,
            @RequestBody @Valid MeetingActionRateReqDto dto) {
        return ApiResponse.success(meetingActionService.rateAction(meetingActionId, dto), "액션 아이템 평가가 완료되었습니다.");
    }

    // 액션 아이템 결재 연결
    @CheckPermission(resource = Resource.GOAL, action = Action.UPDATE)
    @PatchMapping("/{meetingRecordId}/action/{meetingActionId}/approval")
    public ApiResponse<MeetingActionResDto> linkApproval(
            @PathVariable UUID meetingRecordId,
            @PathVariable UUID meetingActionId,
            @RequestBody @Valid LinkApprovalReqDto dto) {
        return ApiResponse.success(meetingActionService.linkApproval(meetingActionId, dto.getApprovalId()), "결재가 연결되었습니다.");
    }

    // 액션 아이템 삭제
    @CheckPermission(resource = Resource.GOAL, action = Action.DELETE)
    @DeleteMapping("/{meetingRecordId}/action/{meetingActionId}")
    public ApiResponse<Void> deleteAction(
            @PathVariable UUID meetingRecordId,
            @PathVariable UUID meetingActionId) {
        meetingActionService.deleteAction(meetingActionId);
        return ApiResponse.success(null, "액션 아이템이 삭제되었습니다.");
    }
}
