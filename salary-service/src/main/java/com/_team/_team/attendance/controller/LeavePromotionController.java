package com._team._team.attendance.controller;

import com._team._team.attendance.dto.reqDto.LeavePromotionDesignateReqDto;
import com._team._team.attendance.dto.reqDto.LeavePromotionRespondReqDto;
import com._team._team.attendance.dto.resDto.LeavePromotionHistoryResDto;
import com._team._team.attendance.dto.resDto.LeavePromotionMyResDto;
import com._team._team.attendance.dto.resDto.LeavePromotionNoResponseResDto;
import com._team._team.attendance.service.LeavePromotionResponseService;
import com._team._team.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// 연차 사용 촉진 통보 회신 강제 지정 API
@RestController
@RequestMapping("/leave-promotions")
@RequiredArgsConstructor
public class LeavePromotionController {

    private final LeavePromotionResponseService service;

    // 직원 본인이 받은 통보 목록
    @GetMapping("/my")
    public ResponseEntity<?> findMy(@RequestHeader("X-User-Id") UUID memberId) {
        List<LeavePromotionMyResDto> data = service.findMyPromotions(memberId);
        return new ResponseEntity<>(
                ApiResponse.success(data, "내 촉진 통보 조회 성공"),
                HttpStatus.OK);
    }

    // 직원 사용 계획 회신
    @PostMapping("/{promotionLogId}/respond")
    public ResponseEntity<?> respond(@RequestHeader("X-User-Id") UUID memberId,
                                     @PathVariable UUID promotionLogId,
                                     @Valid @RequestBody LeavePromotionRespondReqDto reqDto) {
        service.respondAsMember(memberId, promotionLogId, reqDto);
        return new ResponseEntity<>(
                ApiResponse.success(null, "사용 계획 회신 완료"),
                HttpStatus.OK);
    }

    // 관리자 무응답자 리스트
    @GetMapping("/admin/no-response")
    public ResponseEntity<?> findNoResponse(@RequestHeader("X-User-CompanyId") UUID companyId) {
        List<LeavePromotionNoResponseResDto> data = service.findNoResponse(companyId, LocalDate.now());
        return new ResponseEntity<>(
                ApiResponse.success(data, "무응답자 조회 성공"),
                HttpStatus.OK);
    }

    // 관리자 — 통보 이력 (회신 완료 + 강제 지정)
    @GetMapping("/admin/history")
    public ResponseEntity<?> findHistory(@RequestHeader("X-User-CompanyId") UUID companyId) {
        List<LeavePromotionHistoryResDto> data = service.findHistory(companyId);
        return new ResponseEntity<>(
                ApiResponse.success(data, "촉진 이력 조회 성공"),
                HttpStatus.OK);
    }

    // 관리자 강제 지정 노무수령 거부
    @PostMapping("/{promotionLogId}/designate")
    public ResponseEntity<?> designate(@RequestHeader("X-User-Id") UUID adminId,
                                       @RequestHeader("X-User-CompanyId") UUID companyId,
                                       @PathVariable UUID promotionLogId,
                                       @Valid @RequestBody LeavePromotionDesignateReqDto reqDto) {
        service.designateByAdmin(adminId, companyId, promotionLogId, reqDto, LocalDate.now());
        return new ResponseEntity<>(
                ApiResponse.success(null, "강제 지정 완료"),
                HttpStatus.OK);
    }
}