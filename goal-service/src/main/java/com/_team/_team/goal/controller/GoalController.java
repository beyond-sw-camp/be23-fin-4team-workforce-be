package com._team._team.goal.controller;

import com._team._team.annotation.Action;
import com._team._team.annotation.CheckPermission;
import com._team._team.annotation.Resource;
import com._team._team.dto.ApiResponse;
import com._team._team.goal.dto.reqdto.GoalCheckinReqDto;
import com._team._team.goal.dto.reqdto.GoalCreateReqDto;
import com._team._team.goal.service.GoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/goal")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    // 목표 생성
    @CheckPermission(resource = Resource.GOAL, action = Action.CREATE)
    @PostMapping
    public ResponseEntity<?> createGoal(
            @RequestHeader("X-User-UUID") UUID memberId,
            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
            @Valid @RequestBody GoalCreateReqDto reqDto
    ) {
        return new ResponseEntity<>(
                ApiResponse.success(goalService.createGoal(reqDto), "목표 생성 성공"),
                HttpStatus.CREATED
        );
    }

    // 목표 목록 조회
    @CheckPermission(resource = Resource.GOAL, action = Action.READ)
    @GetMapping
    public ResponseEntity<?> getGoalList(
            @RequestHeader("X-User-UUID") UUID memberId,
            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
            @RequestHeader("X-User-CompanyId") UUID companyId
    ) {
        return new ResponseEntity<>(
                ApiResponse.success(goalService.getGoalList(companyId), "목표 목록 조회 성공"),
                HttpStatus.OK
        );
    }

    // 목표 상세 조회
    @CheckPermission(resource = Resource.GOAL, action = Action.READ)
    @GetMapping("/{goalId}")
    public ResponseEntity<?> getGoalDetail(
            @RequestHeader("X-User-UUID") UUID memberId,
            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
            @PathVariable UUID goalId
    ) {
        return new ResponseEntity<>(
                ApiResponse.success(goalService.getGoalDetail(goalId), "목표 상세 조회 성공"),
                HttpStatus.OK
        );
    }

    // 목표 체크인
    @CheckPermission(resource = Resource.GOAL, action = Action.UPDATE)
    @PostMapping("/{goalId}/checkin")
    public ResponseEntity<?> checkinGoal(
            @RequestHeader("X-User-UUID") UUID memberId,
            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
            @PathVariable UUID goalId,
            @Valid @RequestBody GoalCheckinReqDto reqDto
    ) {
        goalService.checkinGoal(memberId, goalId, reqDto);
        return new ResponseEntity<>(
                ApiResponse.success(null, "목표 체크인 성공"),
                HttpStatus.OK
        );
    }
}
