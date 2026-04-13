package com._team._team.evaluation.controller;

import com._team._team.annotation.Action;
import com._team._team.annotation.CheckPermission;
import com._team._team.annotation.Resource;
import com._team._team.dto.ApiResponse;
import com._team._team.evaluation.domain.enums.SeasonPhase;
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

    @CheckPermission(resource = Resource.EVALUATION, action = Action.UPDATE)
    @PostMapping("/{seasonId}/close")
    public ResponseEntity<ApiResponse<SeasonResDto>> closeSeason(
            @RequestHeader("X-User-CompanyId") String companyId,
            @PathVariable UUID seasonId) {
        SeasonResDto result = seasonService.closeSeason(seasonId, UUID.fromString(companyId));
        return ResponseEntity.ok(ApiResponse.success(result, "평가 시즌이 종료되었습니다."));
    }

    /**
     * 시즌 페이즈 전이 (SELF_EVAL → PEER_EVAL → ... → CALIBRATION).
     * CONFIRMED / PUBLISHED 는 별도 엔드포인트(캘리브레이션 확정, 결과 공개)를 사용
     */
    @CheckPermission(resource = Resource.EVALUATION, action = Action.UPDATE)
    @PostMapping("/{seasonId}/phase")
    public ResponseEntity<ApiResponse<SeasonResDto>> transitionPhase(
            @RequestHeader("X-User-CompanyId") String companyId,
            @PathVariable UUID seasonId,
            @RequestParam("next") SeasonPhase next) {
        SeasonResDto result = seasonService.transitionPhase(seasonId, UUID.fromString(companyId), next);
        return ResponseEntity.ok(ApiResponse.success(result, "평가 페이즈가 전이되었습니다."));
    }

    /**
     * 평가 결과 공개 — PUBLISHED 로 전이하고 피드백 면담 자동 생성 이벤트를 발행
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
