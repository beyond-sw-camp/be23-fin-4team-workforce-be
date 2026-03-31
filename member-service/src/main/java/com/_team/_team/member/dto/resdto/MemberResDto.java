package com._team._team.member.dto.resdto;

import com._team._team.member.domain.Member;
import com._team._team.member.domain.MemberPosition;
import com._team._team.member.domain.enums.AccountStatus;
import com._team._team.member.domain.enums.EmploymentType;
import com._team._team.member.domain.enums.MemberStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberResDto {

    private UUID memberId;
    private String email;
    private String name;
    private String sabun;
    private LocalDate joinDate;
    private EmploymentType employmentType;
    private MemberStatus memberStatus;
    private AccountStatus accountStatus;
    private String phoneNumber;
    private String phonePublicYn;
    private String address;
    private String addressPublicYn;
    private String bank;
    private String bankAccount;
    private String profileUrl;
    private String extensionNumber;
    private String telNumber;

    // 포지션 정보 ← 추가
    private UUID memberPositionId;
    private String organizationName;
    private String jobGradeName;
    private String jobTitleName;
    private String roleName;


    // fromEntity
    public static MemberResDto fromEntity(Member member) {
        return MemberResDto.builder()
                .memberId(member.getMemberId())
                .email(member.getEmail())
                .name(member.getName())
                .sabun(member.getSabun())
                .joinDate(member.getJoinDate())
                .employmentType(member.getEmploymentType())
                .memberStatus(member.getMemberStatus())
                .accountStatus(member.getAccountStatus())
                .phoneNumber(member.getPhoneNumber())
                .phonePublicYn(member.getPhonePublicYn())
                .address(member.getAddress())
                .addressPublicYn(member.getAddressPublicYn())
                .bank(member.getBank())
                .bankAccount(member.getBankAccount())
                .profileUrl(member.getProfileUrl())
                .extensionNumber(member.getExtensionNumber())
                .telNumber(member.getTelNumber())
                .build();
    }

    // 포지션 정보 포함 버전
    public static MemberResDto fromEntity(Member member, MemberPosition position) {
        return MemberResDto.builder()
                .memberId(member.getMemberId())
                .email(member.getEmail())
                .name(member.getName())
                .sabun(member.getSabun())
                .joinDate(member.getJoinDate())
                .employmentType(member.getEmploymentType())
                .memberStatus(member.getMemberStatus())
                .accountStatus(member.getAccountStatus())
                .phoneNumber(member.getPhoneNumber())
                .phonePublicYn(member.getPhonePublicYn())
                .address(member.getAddress())
                .addressPublicYn(member.getAddressPublicYn())
                .bank(member.getBank())
                .bankAccount(member.getBankAccount())
                .profileUrl(member.getProfileUrl())
                .extensionNumber(member.getExtensionNumber())
                .telNumber(member.getTelNumber())
                .memberPositionId(position.getMemberPositionId())
                .organizationName(position.getOrganization().getName())
                .jobGradeName(position.getJobGrade().getName())
                .jobTitleName(position.getJobTitle().getName())
                .roleName(position.getRole().getName())
                .build();
    }
}
