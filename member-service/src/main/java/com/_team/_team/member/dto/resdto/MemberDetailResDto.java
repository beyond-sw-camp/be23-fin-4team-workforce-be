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

    private UUID memberId;
    private String email;
    private String name;
    private String sabun;
    private LocalDate joinDate;
    private EmploymentType employmentType;
    private MemberStatus memberStatus;
    private AccountStatus accountStatus;
    private String profileUrl;
    private String extensionNumber;
    private String telNumber;

    // phonePublicYn = YES 일 때만 노출
    private String phoneNumber;

    // addressPublicYn = YES 일 때만 노출
    private String address;
    private String detailAddress;

    // 본인 조회 시에만 노출
    private String emergencyContact;
    private String bank;
    private String bankAccount;

    // 조직 정보
    private UUID memberPositionId;
    private String organizationName;
    private String jobGradeName;
    private String jobTitleName;
    private String roleName;
    private String isSystemAdminYn;

    // 타인 조회 (공개 여부 체크)
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
                .profileUrl(member.getProfileUrl())
                .extensionNumber(member.getExtensionNumber())
                .telNumber(member.getTelNumber())
                .phoneNumber("YES".equals(member.getPhonePublicYn())
                        ? member.getPhoneNumber() : null)
                .address("YES".equals(member.getAddressPublicYn())
                        ? member.getAddress() : null)
                .detailAddress("YES".equals(member.getAddressPublicYn())
                        ? member.getDetailAddress() : null)
                // 타인 조회 시 비상연락처, 은행 정보 미노출
                .emergencyContact(null)
                .bank(null)
                .bankAccount(null)
                .memberPositionId(position.getMemberPositionId())
                .organizationName(position.getOrganization().getName())
                .jobGradeName(position.getJobGrade().getName())
                .jobTitleName(position.getJobTitle().getName())
                .roleName(position.getRole().getName())
                .isSystemAdminYn(position.getIsSystemAdminYn())
                .build();
    }

    // 본인 조회 (전체 정보 노출)
    public static MemberDetailResDto fromEntitySelf(Member member, MemberPosition position) {
        return MemberDetailResDto.builder()
                .memberId(member.getMemberId())
                .email(member.getEmail())
                .name(member.getName())
                .sabun(member.getSabun())
                .joinDate(member.getJoinDate())
                .employmentType(member.getEmploymentType())
                .memberStatus(member.getMemberStatus())
                .accountStatus(member.getAccountStatus())
                .profileUrl(member.getProfileUrl())
                .extensionNumber(member.getExtensionNumber())
                .telNumber(member.getTelNumber())
                .phoneNumber(member.getPhoneNumber())
                .address(member.getAddress())
                .detailAddress(member.getDetailAddress())
                .emergencyContact(member.getEmergencyContact())
                .bank(member.getBank())
                .bankAccount(member.getBankAccount())
                .memberPositionId(position.getMemberPositionId())
                .organizationName(position.getOrganization().getName())
                .jobGradeName(position.getJobGrade().getName())
                .jobTitleName(position.getJobTitle().getName())
                .roleName(position.getRole().getName())
                .isSystemAdminYn(position.getIsSystemAdminYn())
                .build();
    }
}