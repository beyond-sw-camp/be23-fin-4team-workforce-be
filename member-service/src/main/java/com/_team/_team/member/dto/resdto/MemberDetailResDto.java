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
public class MemberDetailResDto {

    // 기본 정보
    private UUID memberId;
    private String email;
    private String name;
    private String sabun;
    private LocalDate joinDate;
    private EmploymentType employmentType;
    private MemberStatus memberStatus;
    private AccountStatus accountStatus;

    // 연락처
    private String phoneNumber;
    private String phonePublicYn;
    private String emergencyContact;

    // 주소
    private String address;
    private String detailAddress;
    private String addressPublicYn;

    // 급여
    private String bank;
    private String bankAccount;

    // 기타
    private String profileUrl;
    private String extensionNumber;
    private String telNumber;

    // 조직 정보
    private UUID memberPositionId;
    private String organizationName;
    private String jobGradeName;
    private String jobTitleName;
    private String roleName;
    private String isSystemAdminYn;

    public static MemberDetailResDto fromEntity(Member member, MemberPosition position) {
        return MemberDetailResDto.builder()
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
                .emergencyContact(member.getEmergencyContact())
                .address(member.getAddress())
                .detailAddress(member.getDetailAddress())
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
                .isSystemAdminYn(position.getIsSystemAdminYn())
                .build();
    }
}