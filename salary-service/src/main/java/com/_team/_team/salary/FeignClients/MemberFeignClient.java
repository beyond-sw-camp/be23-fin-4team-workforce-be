package com._team._team.salary.FeignClients;

import com._team._team.dto.ApiResponse;
import com._team._team.salary.FeignClients.dto.MemberResDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "member-service")
public interface MemberFeignClient {

    @GetMapping("/member/{targetMemberId}")
    ApiResponse<MemberResDto> getMember(
            @RequestHeader("X-User-UUID") UUID memberId,
            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
            @PathVariable UUID targetMemberId);

    @GetMapping("/member/list")
    ApiResponse<List<MemberResDto>> getMemberList(
            @RequestHeader("X-User-UUID") UUID memberId,
            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId);

    /**
     * 회사 소속 재직 사원 전체 조회
     * - 배치 스케줄러에서 호출
     */
    @GetMapping("/member/internal/by-company")
    ApiResponse<List<MemberResDto>> getMembersByCompany(
            @RequestHeader("X-User-CompanyId") UUID companyId);
}
