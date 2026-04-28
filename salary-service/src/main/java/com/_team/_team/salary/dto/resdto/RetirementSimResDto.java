package com._team._team.salary.dto.resdto;


import com._team._team.salary.domain.enums.RetirementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

// 퇴직금 시뮬 응답
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class RetirementSimResDto {

    // 회사 정책
    private RetirementType retirementType;
    private String modeDescription;

    // 직원 정보
    private UUID memberId;
    private String memberName;
    private LocalDate joinDate;        // 정산 시작일
    private LocalDate resignDate;      // 예상 퇴직일

    // 근속 / 급여
    private long serviceDays;          // 재직일수
    private long avgMonthlyWage;       // 평균 월급

    // 결과
    private long estimatedAmount;      // 예상 퇴직금
    private boolean eligible;          // 1년 이상 자격 충족 여부

    // 안내
    private String disclaimer;
}