package com._team._team.salary.FeignClients.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberResDto {
    private UUID memberId;
    private String name;
    private String sabun;
    private String employmentType;
    private LocalDate joinDate;
    private String bank;
    private String bankAccount;
    private String phoneNumber;
    private String email;
}