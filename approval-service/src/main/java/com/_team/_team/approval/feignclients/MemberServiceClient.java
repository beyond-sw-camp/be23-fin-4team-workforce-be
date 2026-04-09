package com._team._team.approval.feignclients;

import com._team._team.approval.feignclients.dto.MemberPositionResDto;
import com._team._team.approval.feignclients.dto.OrganizationResDto;
import com._team._team.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

@Service
public class MemberServiceClient {

    private final RestTemplate restTemplate;

    @Autowired
    public MemberServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }


    // 요청자 MemberPosition 단건 조회
    public MemberPositionResDto getMemberPosition(UUID memberPositionId) {
        String endPoint = "http://member-service/member/position/" + memberPositionId;
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);

        ResponseEntity<ApiResponse<MemberPositionResDto>> response =
                restTemplate.exchange(endPoint, HttpMethod.GET, httpEntity,
                        new ParameterizedTypeReference<ApiResponse<MemberPositionResDto>>() {});

        return response.getBody().getData();
    }

    // 조직 + 직책 기반 후보 조회
    public List<MemberPositionResDto> searchByJobTitle(UUID organizationId, UUID jobTitleId) {
        String endPoint = "http://member-service/member/position/search/by-job-title"
                + "?organizationId=" + organizationId
                + "&jobTitleId=" + jobTitleId;
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);

        ResponseEntity<ApiResponse<List<MemberPositionResDto>>> response =
                restTemplate.exchange(endPoint, HttpMethod.GET, httpEntity,
                        new ParameterizedTypeReference<ApiResponse<List<MemberPositionResDto>>>() {});

        return response.getBody().getData();
    }

    // 조직 단건 조회
    public OrganizationResDto getOrganization(UUID organizationId) {
        String endPoint = "http://member-service/member/organization/" + organizationId;
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);

        ResponseEntity<ApiResponse<OrganizationResDto>> response =
                restTemplate.exchange(endPoint, HttpMethod.GET, httpEntity,
                        new ParameterizedTypeReference<ApiResponse<OrganizationResDto>>() {});

        return response.getBody().getData();
    }
}
