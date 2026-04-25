package com._team._team.attendance.controller;

import com._team._team.attendance.dto.reqDto.CompanyIpWhitelistCreateReqDto;
import com._team._team.attendance.dto.reqDto.CompanyIpWhitelistUpdateReqDto;
import com._team._team.attendance.dto.resDto.CompanyIpWhitelistResDto;
import com._team._team.attendance.service.CompanyIpWhitelistService;
import com._team._team.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/attendance/ip-whitelist")
public class CompanyIpWhitelistController {

    private final CompanyIpWhitelistService service;

    @Autowired
    public CompanyIpWhitelistController(CompanyIpWhitelistService service) {
        this.service = service;
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @Valid @RequestBody CompanyIpWhitelistCreateReqDto reqDto) {
        CompanyIpWhitelistResDto res = CompanyIpWhitelistResDto.fromEntity(
                service.create(companyId, reqDto));
        return new ResponseEntity<>(
                ApiResponse.success(res, "허용 IP 가 등록되었습니다."),
                HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<?> findAll(
            @RequestHeader("X-User-CompanyId") UUID companyId) {
        List<CompanyIpWhitelistResDto> list = service.findAllByCompany(companyId)
                .stream()
                .map(CompanyIpWhitelistResDto::fromEntity)
                .toList();
        return new ResponseEntity<>(
                ApiResponse.success(list, "허용 IP 목록 조회 성공"),
                HttpStatus.OK);
    }

    @PutMapping("/{companyIpWhitelistId}")
    public ResponseEntity<?> update(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID companyIpWhitelistId,
            @Valid @RequestBody CompanyIpWhitelistUpdateReqDto reqDto) {
        CompanyIpWhitelistResDto res = CompanyIpWhitelistResDto.fromEntity(
                service.update(companyId, companyIpWhitelistId, reqDto));
        return new ResponseEntity<>(
                ApiResponse.success(res, "허용 IP 가 수정되었습니다."),
                HttpStatus.OK);
    }

    @DeleteMapping("/{companyIpWhitelistId}")
    public ResponseEntity<?> delete(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID companyIpWhitelistId) {
        service.delete(companyId, companyIpWhitelistId);
        return new ResponseEntity<>(
                ApiResponse.success(null, "허용 IP 가 삭제되었습니다."),
                HttpStatus.OK);
    }
}