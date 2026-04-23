package com._team._team.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// 신규 입사 이벤트 member-service → salary-service
// Salary 자동 생성 트리거
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class MemberHiredEvent {

    public static final String TOPIC = "member-hired";

    private UUID memberId;
    private UUID companyId;
    private UUID jobGradeId;
    private String jobGradeName;
    private String jobTitleName;
    private LocalDate hireDate;
    private Long baseSalary;
    private Integer step;
    private Instant eventTime;
}
