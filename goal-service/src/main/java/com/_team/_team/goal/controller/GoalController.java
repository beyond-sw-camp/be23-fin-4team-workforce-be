package com._team._team.goal.controller;

import com._team._team.annotation.Action;
import com._team._team.annotation.CheckPermission;
import com._team._team.annotation.Resource;
import com._team._team.goal.permission.PositionPermissionReader;
import com._team._team.dto.ApiResponse;
import com._team._team.goal.domain.enums.GoalHealthStatus;
import com._team._team.goal.domain.enums.KpiCycle;
import com._team._team.goal.dto.reqdto.*;
import com._team._team.goal.dto.resdto.GoalCommentResDto;
import com._team._team.goal.dto.resdto.GoalProgressUpdateResDto;
import com._team._team.goal.dto.resdto.GoalResDto;
import com._team._team.goal.service.GoalCommentService;
import com._team._team.goal.service.GoalCompletionFileService;
import com._team._team.goal.service.GoalProgressUpdateService;
import com._team._team.goal.service.GoalService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// 목표 조회/수정
@RestController
@RequestMapping("/goal")
public class GoalController {

    final GoalService goalService;
    final GoalProgressUpdateService goalProgressUpdateService;
    final GoalCommentService goalCommentService;
    final GoalCompletionFileService goalCompletionFileService;
    final PositionPermissionReader positionPermissionReader;

    @Autowired
    public GoalController(
            GoalService goalService,
            GoalProgressUpdateService goalProgressUpdateService,
            GoalCommentService goalCommentService,
            GoalCompletionFileService goalCompletionFileService,
            PositionPermissionReader positionPermissionReader) {
        this.goalService = goalService;
        this.goalProgressUpdateService = goalProgressUpdateService;
        this.goalCommentService = goalCommentService;
        this.goalCompletionFileService = goalCompletionFileService;
        this.positionPermissionReader = positionPermissionReader;
    }

    // 목표 생성
    @CheckPermission(resource = Resource.GOAL, action = Action.CREATE)
    @PostMapping
    public ApiResponse<GoalResDto> createGoal(
            @RequestHeader("X-User-UUID") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId,
            @RequestBody @Valid GoalCreateReqDto dto) {
        return ApiResponse.success(
                goalService.createGoal(dto, UUID.fromString(memberId), UUID.fromString(companyId)),
                "목표가 생성되었습니다.");
    }

    // 목표 목록 조회
    @CheckPermission(resource = Resource.GOAL, action = Action.READ)
    @GetMapping
    public ApiResponse<List<GoalResDto>> listGoals(
            @RequestHeader("X-Member-Id") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId,
            @RequestParam(required = false) UUID parentId,
            @RequestParam(required = false) KpiCycle cycle,
            @RequestParam(required = false) UUID ownerId,
            @RequestParam(required = false) GoalHealthStatus status,
            @RequestParam(required = false) Integer depth) {
        return ApiResponse.success(
                goalService.listGoals(
                        UUID.fromString(companyId),
                        UUID.fromString(memberId),
                        UUID.fromString(companyId),
                        parentId,
                        cycle,
                        ownerId,
                        status,
                        depth),
                "목표 목록 조회 성공");
    }

    // 목표 트리 조회
    @CheckPermission(resource = Resource.GOAL, action = Action.READ)
    @GetMapping("/tree")
    public ApiResponse<List<GoalResDto>> treeGoals(
            @RequestHeader("X-Member-Id") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId,
            @RequestParam(required = false, defaultValue = "all") String scope,
            @RequestParam(required = false) UUID orgId,
            @RequestParam(required = false) UUID ownerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd) {
        return ApiResponse.success(
                goalService.treeGoals(
                        UUID.fromString(companyId),
                        UUID.fromString(memberId),
                        UUID.fromString(companyId),
                        scope,
                        ownerId != null ? ownerId : orgId,
                        periodStart,
                        periodEnd),
                "목표 트리 조회 성공");
    }

    // 목표 상세 조회
    @CheckPermission(resource = Resource.GOAL, action = Action.READ)
    @GetMapping("/{goalId}")
    public ApiResponse<GoalResDto> getGoal(
            @PathVariable UUID goalId,
            @RequestHeader("X-User-UUID") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId) {
        return ApiResponse.success(
                goalService.getGoal(goalId, UUID.fromString(memberId), UUID.fromString(companyId)),
                "목표 조회 성공");
    }

    // 목표 수정
    @CheckPermission(resource = Resource.GOAL, action = Action.READ)
    @PatchMapping("/{goalId}")
    public ApiResponse<GoalResDto> updateGoal(
            @PathVariable UUID goalId,
            @RequestBody @Valid GoalUpdateReqDto dto,
            @RequestHeader("X-Member-Id") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId,
            HttpServletRequest request) {
        boolean hasGoalUpdate = positionPermissionReader.hasPermission(request, Resource.GOAL, Action.UPDATE);
        return ApiResponse.success(
                goalService.updateGoal(
                        goalId,
                        dto,
                        UUID.fromString(companyId),
                        UUID.fromString(memberId),
                        hasGoalUpdate),
                "목표가 수정되었습니다.");
    }

    // 목표 삭제
    @CheckPermission(resource = Resource.GOAL, action = Action.DELETE)
    @DeleteMapping("/{goalId}")
    public ApiResponse<Void> deleteGoal(
            @PathVariable UUID goalId,
            @RequestHeader("X-User-CompanyId") String companyId) {
        goalService.deleteGoal(goalId, UUID.fromString(companyId));
        return ApiResponse.success(null, "목표가 삭제되었습니다.");
    }

    // 목표 활성화
    @CheckPermission(resource = Resource.GOAL, action = Action.UPDATE)
    @PatchMapping("/{goalId}/activate")
    public ApiResponse<GoalResDto> activateGoal(
            @PathVariable UUID goalId,
            @RequestHeader("X-User-UUID") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId) {
        return ApiResponse.success(
                goalService.activateGoal(goalId, UUID.fromString(memberId), UUID.fromString(companyId)),
                "목표가 활성화되었습니다.");
    }

    // 목표 완료
    @CheckPermission(resource = Resource.GOAL, action = Action.UPDATE)
    @PatchMapping("/{goalId}/complete")
    public ApiResponse<GoalResDto> completeGoal(
            @PathVariable UUID goalId,
            @RequestHeader("X-User-CompanyId") String companyId) {
        return ApiResponse.success(
                goalService.completeGoal(goalId, UUID.fromString(companyId)),
                "목표가 완료되었습니다.");
    }

    // 목표 취소
    @CheckPermission(resource = Resource.GOAL, action = Action.UPDATE)
    @PatchMapping("/{goalId}/cancel")
    public ApiResponse<GoalResDto> cancelGoal(
            @PathVariable UUID goalId,
            @RequestHeader("X-User-CompanyId") String companyId) {
        return ApiResponse.success(
                goalService.cancelGoal(goalId, UUID.fromString(companyId)),
                "목표가 취소되었습니다.");
    }

    // 목표 롤업 재계산
    @CheckPermission(resource = Resource.GOAL, action = Action.UPDATE)
    @PostMapping("/{goalId}/recalculate")
    public ApiResponse<GoalResDto> recalculate(
            @PathVariable UUID goalId,
            @RequestHeader("X-User-CompanyId") String companyId) {
        return ApiResponse.success(
                goalService.recalculate(goalId, UUID.fromString(companyId)),
                "목표 롤업 재계산 완료");
    }

    // 진행률 업데이트 등록
    /** 담당/참여는 READ로 진입 가능. 대리 갱신은 GOAL:UPDATE 필요. */
    // 완료 제출 등록
    @CheckPermission(resource = Resource.GOAL, action = Action.READ)
    @PostMapping("/{goalId}/updates")
    public ApiResponse<GoalProgressUpdateResDto> addProgressUpdate(
            @PathVariable UUID goalId,
            @RequestBody @Valid GoalProgressUpdateReqDto dto,
            @RequestHeader("X-Member-Id") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId,
            HttpServletRequest request) {
        boolean hasGoalUpdate = positionPermissionReader.hasPermission(request, Resource.GOAL, Action.UPDATE);
        return ApiResponse.success(
                goalProgressUpdateService.addUpdate(
                        goalId,
                        dto,
                        UUID.fromString(memberId),
                        UUID.fromString(companyId),
                        hasGoalUpdate),
                "진행률이 업데이트되었습니다.");
    }

    // 완료 제출 등록
    @CheckPermission(resource = Resource.GOAL, action = Action.READ)
    @PostMapping("/{goalId}/completion/submit")
    public ApiResponse<GoalResDto> submitCompletion(
            @PathVariable UUID goalId,
            @RequestBody @Valid GoalCompletionSubmitReqDto dto,
            @RequestHeader("X-Member-Id") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId,
            HttpServletRequest request) {
        boolean hasGoalUpdate = positionPermissionReader.hasPermission(request, Resource.GOAL, Action.UPDATE);
        return ApiResponse.success(
                goalService.submitCompletionForApproval(
                        goalId,
                        dto,
                        UUID.fromString(memberId),
                        UUID.fromString(companyId),
                        hasGoalUpdate),
                "완료 제출이 등록되었습니다.");
    }

    // 완료 제출 파일 업로드
    @CheckPermission(resource = Resource.GOAL, action = Action.READ)
    @PostMapping("/completion/files")
    public ApiResponse<List<String>> uploadCompletionFiles(
            @RequestParam("files") List<MultipartFile> files) {
        return ApiResponse.success(
                goalCompletionFileService.upload(files),
                "완료 제출 파일 업로드 성공");
    }

    @CheckPermission(resource = Resource.GOAL, action = Action.READ)
    @GetMapping("/{goalId}/updates")
    public ApiResponse<List<GoalProgressUpdateResDto>> listProgressUpdates(
            @PathVariable UUID goalId,
            @RequestHeader("X-User-CompanyId") String companyId) {
        return ApiResponse.success(
                goalProgressUpdateService.list(goalId, UUID.fromString(companyId)),
                "진행률 이력 조회 성공");
    }

    // 댓글 등록
    @CheckPermission(resource = Resource.GOAL, action = Action.CREATE)
    @PostMapping("/{goalId}/comments")
    public ApiResponse<GoalCommentResDto> addComment(
            @PathVariable UUID goalId,
            @RequestBody @Valid GoalCommentReqDto dto,
            @RequestHeader("X-Member-Id") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId) {
        return ApiResponse.success(
                goalCommentService.create(goalId, dto, UUID.fromString(memberId), UUID.fromString(companyId)),
                "댓글이 등록되었습니다.");
    }

    // 댓글 목록 조회
    @CheckPermission(resource = Resource.GOAL, action = Action.READ)
    @GetMapping("/{goalId}/comments")
    public ApiResponse<List<GoalCommentResDto>> listComments(
            @PathVariable UUID goalId,
            @RequestHeader("X-User-CompanyId") String companyId) {
        return ApiResponse.success(goalCommentService.list(goalId, UUID.fromString(companyId)), "댓글 목록 조회 성공");
    }

    // 댓글 수정
    @CheckPermission(resource = Resource.GOAL, action = Action.UPDATE)
    @PatchMapping("/{goalId}/comments/{commentId}")
    public ApiResponse<GoalCommentResDto> updateComment(
            @PathVariable UUID goalId,
            @PathVariable UUID commentId,
            @RequestBody @Valid GoalCommentReqDto dto,
            @RequestHeader("X-Member-Id") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId) {
        return ApiResponse.success(
                goalCommentService.update(goalId, commentId, dto, UUID.fromString(memberId), UUID.fromString(companyId)),
                "댓글이 수정되었습니다.");
    }

    // 댓글 삭제
    @CheckPermission(resource = Resource.GOAL, action = Action.DELETE)
    @DeleteMapping("/{goalId}/comments/{commentId}")
    public ApiResponse<Void> deleteComment(
            @PathVariable UUID goalId,
            @PathVariable UUID commentId,
            @RequestHeader("X-Member-Id") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId) {
        goalCommentService.delete(goalId, commentId, UUID.fromString(memberId), UUID.fromString(companyId));
        return ApiResponse.success(null, "댓글이 삭제되었습니다.");
    }

    // 댓글 리액션 토글
    @CheckPermission(resource = Resource.GOAL, action = Action.UPDATE)
    @PostMapping("/{goalId}/comments/{commentId}/reactions")
    public ApiResponse<GoalCommentResDto> toggleReaction(
            @PathVariable UUID goalId,
            @PathVariable UUID commentId,
            @RequestBody @Valid GoalCommentReactionReqDto dto,
            @RequestHeader("X-User-CompanyId") String companyId) {
        return ApiResponse.success(
                goalCommentService.toggleReaction(goalId, commentId, dto, UUID.fromString(companyId)),
                "리액션이 반영되었습니다.");
    }
}