package com._team._team.approval.feignclients;

import com._team._team.approval.feignclients.dto.MemberPositionResDto;
import com._team._team.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "member-service")
public interface MemberServiceFeignClient {

    // 요청자 MemberPosition 단건 조회
    @GetMapping("/member/position/{memberPositionId}")
    ApiResponse<MemberPositionResDto> getMemberPosition(
            @PathVariable("memberPositionId") UUID memberPositionId);


    // 조직 + 직급 기반 결재자 후보 조회
    @GetMapping("/member/position/search/by-job-grade")
    ApiResponse<List<MemberPositionResDto>> searchByJobGrade(
            @RequestParam("organizationId") UUID organizationId,
            @RequestParam("jobGradeId") UUID jobGradeId);

}
