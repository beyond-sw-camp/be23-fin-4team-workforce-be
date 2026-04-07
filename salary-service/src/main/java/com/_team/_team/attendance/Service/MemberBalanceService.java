package com._team._team.attendance.Service;

import com._team._team.attendance.Repository.MemberBalanceRepository;
import com._team._team.attendance.domain.MemberBalance;
import com._team._team.attendance.dto.reqDto.MemberBalanceCreateReqDto;
import com._team._team.attendance.dto.resDto.MemberBalanceResDto;
import com._team._team.attendance.dto.reqDto.MemberBalanceUseReqDto;
import com._team._team.dto.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class MemberBalanceService {

    private final MemberBalanceRepository memberBalanceRepository;

    @Autowired
    public MemberBalanceService(MemberBalanceRepository memberBalanceRepository) {
        this.memberBalanceRepository = memberBalanceRepository;
    }

    /**
     * 휴가 부여 (관리자용)
     * → POST /member-balance/grant 호출
     */
    public MemberBalanceResDto grantBalance(UUID companyId, MemberBalanceCreateReqDto reqDto){
        MemberBalance balance = reqDto.toEntity(companyId);
        MemberBalance savedBalance = memberBalanceRepository.save(balance);
        return MemberBalanceResDto.fromEntity(savedBalance);
    }

    // 휴가 조회 (사원용)
    @Transactional(readOnly = true)
    public List<MemberBalanceResDto> findBalances(UUID companyId, UUID memberId){
        List<MemberBalance> balances = memberBalanceRepository
                .findByMemberIdAndCompanyIdAndDelYn(memberId, companyId,"N");
        return balances.stream()
                .map(MemberBalanceResDto::fromEntity)
                .toList();
    }

    /**
     * 휴가 차감 (approval-service 연동)
     *
     * ===== 시나리오 =====
     * 사원이 연차 1일 신청 → approval-service에서 승인
     * → Kafka 이벤트: "memberId=abc, ANNUAL, 1.0일 차감"
     * → 이 메서드 호출
     *
     * ===== 워크플로우 =====
     * [단계 1] findWithLock — 비관적 락으로 해당 사원의 잔여 조회
     * [단계 2] 잔여 일수 검증
     * remaining < days → "잔여 연차가 부족합니다" 400 에러
     */
    public MemberBalanceResDto useBalance(UUID companyId, UUID memberId, MemberBalanceUseReqDto reqDto){

        // 비관적 락으로 잔여 조회
        MemberBalance memberBalance = memberBalanceRepository
                .findWithLock(companyId, memberId, reqDto.getBalanceType())
                .orElseThrow(()-> new BusinessException(
                        HttpStatus.NOT_FOUND, "사용 가능한 휴가 잔여가 없습니다."));

        // 잔여 일수 검증
        if(memberBalance.getRemaining() < reqDto.getDays()){
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "잔여 연차가 부족합니다. (잔여: " + memberBalance.getRemaining() + "일, 요청 : " + reqDto.getDays() + "일)");
        }

        // 휴가 차감
        memberBalance.use(reqDto.getDays());

        return MemberBalanceResDto.fromEntity(memberBalance);
    }

    /**
     * 휴가 복구 (반려/취소 시)
     *
     * ===== 시나리오 =====
     * 승인된 연차 1일이 취소됨 (사원 취소 or 관리자 반려)
     * → Kafka 이벤트: "memberId=abc, ANNUAL, 1.0일 복구"
     * → 이 메서드 호출
     *
     * ===== 차감과 동일하게 비관적 락 사용 =====
     * 복구도 remaining을 변경하는 작업이므로 동시성 보호 필요
     * 차감과 복구가 동시에 일어나도 순차 처리
     */
    public MemberBalanceResDto restoreBalance(UUID companyId, UUID memberId, MemberBalanceUseReqDto reqDto) {

        // 비관적 락으로 잔여 조회
        MemberBalance memberBalance = memberBalanceRepository
                .findWithLock(companyId, memberId, reqDto.getBalanceType())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "복구 대상 휴가 잔여를 찾을 수 없습니다."));

        // 복구
        memberBalance.restore(reqDto.getDays());

        return MemberBalanceResDto.fromEntity(memberBalance);
    }
}
