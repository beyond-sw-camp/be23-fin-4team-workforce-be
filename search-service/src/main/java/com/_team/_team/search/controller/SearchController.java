package com._team._team.search.controller;

import com._team._team.dto.ApiResponse;
import com._team._team.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    // 직원 통합 검색
    @GetMapping("/com/_team/_team/search/employees")
    public ResponseEntity<?> searchEmployees(
            @RequestParam String query,
            @RequestHeader("X-User-CompanyId") String companyId,
            Pageable pageable) {
        return new ResponseEntity<>(
                ApiResponse.success(
                        searchService.searchEmployees(
                                query, companyId, pageable),
                        "직원 검색 성공"),
                HttpStatus.OK
        );
    }

    // 조직 트리 조회
    @GetMapping("/com/_team/_team/search/organization")
    public ResponseEntity<?> getOrganizationTree(
            @RequestHeader("X-User-CompanyId") String companyId) {
        return new ResponseEntity<>(
                ApiResponse.success(
                        searchService.getOrganizationTree(companyId),
                        "조직 검색 성공"),
                HttpStatus.OK
        );
    }

    // 조직별 직원 검색
    @GetMapping("/com/_team/_team/search/employees/organization")
    public ResponseEntity<?> searchEmployeesByOrganization(
            @RequestParam String organizationId,
            @RequestHeader("X-User-CompanyId") UUID companyId,
            Pageable pageable) {
        return new ResponseEntity<>(
                ApiResponse.success(
                        searchService.searchEmployeesByOrganization(
                                organizationId, companyId, pageable),
                        "조직별 직원 검색 성공"),
                HttpStatus.OK
        );
    }
}