package com._team._team.member.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

// createMember 커밋 후 Kafka로 넘기기
@Getter
@AllArgsConstructor
public class MemberHiredInternalEvent {
    private UUID memberId;
    private UUID companyId;
    private UUID jobGradeId;
    private String jobGradeName;
    private String jobTitleName;
    private LocalDate hireDate;
//    private Long baseSalary;
}
