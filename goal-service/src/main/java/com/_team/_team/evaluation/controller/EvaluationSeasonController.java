package com._team._team.evaluation.controller;

import com._team._team.annotation.Action;
import com._team._team.annotation.CheckPermission;
import com._team._team.annotation.Resource;
import com._team._team.dto.ApiResponse;
import com._team._team.evaluation.dto.reqdto.SeasonCreateReqDto;
import com._team._team.evaluation.dto.reqdto.SeasonUpdateReqDto;
import com._team._team.evaluation.dto.resdto.SeasonResDto;
import com._team._team.evaluation.service.EvaluationResponseService;
import com._team._team.evaluation.service.EvaluationSeasonService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/evaluation/evaluation-seasons")
public class EvaluationSeasonController {

    private final EvaluationSeasonService seasonService;
    private final EvaluationResponseService responseService;

    public EvaluationSeasonController(EvaluationSeasonService seasonService,
                                       EvaluationResponseService responseService) {
        this.seasonService = seasonService;
        this.responseService = responseService;
    }

    @CheckPermission(resource = Resource.EVALUATION, action = Action.READ)
    @GetMapping
    public ResponseEntity<ApiResponse<List<SeasonResDto>>> listSeasons(
            @RequestHeader("X-User-CompanyId") String companyId) {
        List<SeasonResDto> result = seasonService.listSeasons(UUID.fromString(companyId));
        return ResponseEntity.ok(ApiResponse.success(result, "평가 시즌 목록 조회 성공"));
    }

    @CheckPermission(resource = Resource.EVALUATION, action = Action.CREATE)
    @PostMapping
    public ResponseEntity<ApiResponse<SeasonResDto>> createSeason(
            @RequestHeader("X-User-CompanyId") String companyId,
            @Valid @RequestBody SeasonCreateReqDto dto) {
        SeasonResDto result = seasonService.createSeason(UUID.fromString(companyId), dto);
        return ResponseEntity.ok(ApiResponse.success(result, "평가 시즌이 생성되었습니다."));
    }

    @CheckPermission(resource = Resource.EVALUATION, action = Action.READ)
    @GetMapping("/{seasonId}")
    public ResponseEntity<ApiResponse<SeasonResDto>> getSeason(
            @RequestHeader("X-User-CompanyId") String companyId,
            @PathVariable UUID seasonId) {
        SeasonResDto result = seasonService.getSeason(seasonId, UUID.fromString(companyId));
        return ResponseEntity.ok(ApiResponse.success(result, "평가 시즌 조회 성공"));
    }

    @CheckPermission(resource = Resource.EVALUATION, action = Action.UPDATE)
    @PatchMapping("/{seasonId}")
    public ResponseEntity<ApiResponse<SeasonResDto>> updateSeason(
            @RequestHeader("X-User-CompanyId") String companyId,
            @PathVariable UUID seasonId,
            @RequestBody SeasonUpdateReqDto dto) {
        SeasonResDto result = seasonService.updateSeason(seasonId, UUID.fromString(companyId), dto);
        return ResponseEntity.ok(ApiResponse.success(result, "평가 시즌이 수정되었습니다."));
    }

    @CheckPermission(resource = Resource.EVALUATION, action = Action.UPDATE)
    @PostMapping("/{seasonId}/start")
    public ResponseEntity<ApiResponse<SeasonResDto>> startSeason(
            @RequestHeader("X-User-CompanyId") String companyId,
            @PathVariable UUID seasonId) {
        SeasonResDto result = seasonService.startSeason(seasonId, UUID.fromString(companyId));
        return ResponseEntity.ok(ApiResponse.success(result, "평가 시즌이 시작되었습니다."));
    }

    /**
     * 시즌 종료 — status 를 CLOSED 로 전이합니다.
     * 선택적으로 publishResults=true 를 주면 종료와 동시에 결과를 공개합니다(미공개 상태인 경우).
     */
    @CheckPermission(resource = Resource.EVALUATION, action = Action.UPDATE)
    @PostMapping("/{seasonId}/close")
    public ResponseEntity<ApiResponse<SeasonResDto>> closeSeason(
            @RequestHeader("X-User-CompanyId") String companyId,
            @PathVariable UUID seasonId,
            @RequestParam(value = "publishResults", defaultValue = "false") boolean publishResults) {
        UUID companyUuid = UUID.fromString(companyId);
        if (publishResults) {
            // 이미 공개된 경우엔 내부에서 예외가 나므로 미공개일 때만 시도
            SeasonResDto current = seasonService.getSeason(seasonId, companyUuid);
            if (current.getResultsPublishedAt() == null) {
                responseService.publishResults(seasonId, companyUuid);
            }
        }
        SeasonResDto result = seasonService.closeSeason(seasonId, companyUuid);
        return ResponseEntity.ok(ApiResponse.success(result, "평가 시즌이 종료되었습니다."));
    }

    /**
     * 평가 결과 공개 — resultsPublishedAt 을 현재 시각으로 세팅하고 피드백 면담 자동 생성.
     */
    @CheckPermission(resource = Resource.EVALUATION, action = Action.UPDATE)
    @PostMapping("/{seasonId}/publish")
    public ResponseEntity<ApiResponse<Void>> publishResults(
            @RequestHeader("X-User-CompanyId") String companyId,
            @PathVariable UUID seasonId) {
        responseService.publishResults(seasonId, UUID.fromString(companyId));
        return ResponseEntity.ok(ApiResponse.success(null, "평가 결과가 공개되었습니다."));
    }
}