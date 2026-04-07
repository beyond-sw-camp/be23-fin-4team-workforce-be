package com._team._team.evaluation.controller;

import com._team._team.annotation.Action;
import com._team._team.annotation.CheckPermission;
import com._team._team.annotation.Resource;
import com._team._team.dto.ApiResponse;
import com._team._team.evaluation.dto.reqdto.CalibrationCreateReqDto;
import com._team._team.evaluation.dto.resdto.CalibrationLogResDto;
import com._team._team.evaluation.service.CalibrationLogService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// 평가 점수 조정(Calibration) 기록 생성 및 조회 컨트롤러
@RestController
@RequestMapping("/evaluation/calibration")
public class CalibrationLogController {

    final CalibrationLogService calibrationLogService;

    @Autowired
    public CalibrationLogController(CalibrationLogService calibrationLogService) {
        this.calibrationLogService = calibrationLogService;
    }

    // 조정 내역 기록
    @CheckPermission(resource = Resource.EVALUATION, action = Action.CREATE)
    @PostMapping
    public ResponseEntity<ApiResponse<CalibrationLogResDto>> createCalibration(
            @RequestHeader("X-Member-Id") String memberId,
            @RequestHeader("X-User-CompanyId") String companyId,
            @Valid @RequestBody CalibrationCreateReqDto dto) {
        CalibrationLogResDto result = calibrationLogService.createCalibration(dto, UUID.fromString(memberId), UUID.fromString(companyId));
        return ResponseEntity.ok(ApiResponse.success(result, "조정 내역이 기록되었습니다."));
    }

    // 평가별 조정 내역 조회
    @CheckPermission(resource = Resource.EVALUATION, action = Action.READ)
    @GetMapping("/{evaluationId}")
    public ResponseEntity<ApiResponse<List<CalibrationLogResDto>>> listByEvaluation(
            @RequestHeader("X-User-CompanyId") String companyId,
            @PathVariable UUID evaluationId) {
        List<CalibrationLogResDto> result = calibrationLogService.listByEvaluation(evaluationId, UUID.fromString(companyId));
        return ResponseEntity.ok(ApiResponse.success(result, "평가별 조정 내역 조회 성공"));
    }
}
