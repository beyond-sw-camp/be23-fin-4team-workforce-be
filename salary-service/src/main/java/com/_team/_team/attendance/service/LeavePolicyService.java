package com._team._team.attendance.service;

import com._team._team.attendance.repository.LeavePolicyRepository;
import com._team._team.attendance.domain.LeavePolicy;
import com._team._team.attendance.dto.reqDto.LeavePolicyCreateReqDto;
import com._team._team.attendance.dto.reqDto.LeavePolicyUpdateReqDto;
import com._team._team.attendance.dto.resDto.LeavePolicyResDto;
import com._team._team.dto.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class LeavePolicyService {
    private final LeavePolicyRepository leavePolicyRepository;

    @Autowired
    public LeavePolicyService(LeavePolicyRepository leavePolicyRepository) {
        this.leavePolicyRepository = leavePolicyRepository;
    }

    /** 연차 정책 생성 */
    public LeavePolicyResDto createPolicy(UUID companyId, LeavePolicyCreateReqDto reqDto){
        LeavePolicy policy = reqDto.toEntity(companyId);
        LeavePolicy savedPolicy = leavePolicyRepository.save(policy);
        return LeavePolicyResDto.fromEntity(savedPolicy);
    }

    /** 연차 정책 전체 목록 조회 */
    @Transactional(readOnly = true)
    public List<LeavePolicyResDto> findPolicies(UUID companyId){
        List<LeavePolicy> leavePolies = leavePolicyRepository.findByCompanyIdAndDelYn(companyId,"N");
        return leavePolies.stream()
                .map(LeavePolicyResDto::fromEntity)
                .toList();
    }

    /** 연차 정책 단건 조회 */
    @Transactional(readOnly = true)
    public LeavePolicyResDto findPolicy(UUID companyId, UUID policyId){
        LeavePolicy leavePolicy = leavePolicyRepository
                .findByCompanyIdAndPolicyIdAndDelYn(companyId, policyId, "N")
                .orElseThrow(()->new BusinessException(
                        HttpStatus.NOT_FOUND, "연차 정책을 찾을 수 없습니다."));
        return LeavePolicyResDto.fromEntity(leavePolicy);
    }

    /** 연차 정책 수정 */
    public LeavePolicyResDto updatePolicy(UUID companyId, UUID policyId, LeavePolicyUpdateReqDto reqDto){
        LeavePolicy leavePolicy = leavePolicyRepository
                .findByCompanyIdAndPolicyIdAndDelYn(companyId, policyId, "N")
                .orElseThrow(()->new BusinessException(
                        HttpStatus.NOT_FOUND, "연차 정책을 찾을 수 없습니다."));
        leavePolicy.update(reqDto);
        return LeavePolicyResDto.fromEntity(leavePolicy);
    }

    /** 연차 정책 삭제 */
    public void deletePolicy(UUID companyId, UUID policyId){
        LeavePolicy leavePolicy = leavePolicyRepository
                .findByCompanyIdAndPolicyIdAndDelYn(companyId, policyId, "N")
                .orElseThrow(()->new BusinessException(
                        HttpStatus.NOT_FOUND, "연차 정책을 찾을 수 없습니다."));
        leavePolicy.delete();
    }
}
