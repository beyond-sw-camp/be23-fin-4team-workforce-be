package com._team._team.organization.controller;

import com._team._team.annotation.Action;
import com._team._team.annotation.CheckPermission;
import com._team._team.annotation.Resource;
import com._team._team.dto.ApiResponse;
import com._team._team.organization.dto.reqdto.OrganizationReqDto;
import com._team._team.organization.service.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class OrganizationController {

    private final OrganizationService organizationService;

    @Autowired
    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @CheckPermission(resource = Resource.ORGANIZATION, action = Action.CREATE)
    @PostMapping("/create")
    public ResponseEntity<?> createOrganization(
            @RequestHeader("X-User-UUID") UUID memberId,
            @Valid @RequestBody OrganizationReqDto reqDto) {
        return new ResponseEntity<>(
                ApiResponse.success(
                        organizationService.createOrganization(memberId, reqDto),
                        "조직 생성 성공"),
                HttpStatus.CREATED
        );
    }

    @CheckPermission(resource = Resource.ORGANIZATION, action = Action.READ)
    @GetMapping("/list")
    public ResponseEntity<?> getOrganizationList(
            @RequestHeader("X-User-UUID") UUID memberId) {
        return new ResponseEntity<>(
                ApiResponse.success(
                        organizationService.getOrganizationList(memberId),
                        "조직 목록 조회 성공"),
                HttpStatus.OK
        );
    }

    @CheckPermission(resource = Resource.ORGANIZATION, action = Action.UPDATE)
    @PutMapping("/{organizationId}")
    public ResponseEntity<?> updateOrganization(
            @RequestHeader("X-User-UUID") UUID memberId,
            @PathVariable UUID organizationId,
            @Valid @RequestBody OrganizationReqDto reqDto) {
        organizationService.updateOrganization(memberId, organizationId, reqDto);
        return new ResponseEntity<>(
                ApiResponse.success(null, "조직 수정 성공"),
                HttpStatus.OK
        );
    }

    @CheckPermission(resource = Resource.ORGANIZATION, action = Action.DELETE)
    @DeleteMapping("/{organizationId}")
    public ResponseEntity<?> deleteOrganization(
            @RequestHeader("X-User-UUID") UUID memberId,
            @PathVariable UUID organizationId) {
        organizationService.deleteOrganization(memberId, organizationId);
        return new ResponseEntity<>(
                ApiResponse.success(null, "조직 삭제 성공"),
                HttpStatus.OK
        );
    }

}
