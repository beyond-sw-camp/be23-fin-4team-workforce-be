package com._team._team.member.domain;

import com._team._team.company.domain.Company;
import com._team._team.domain.BaseTimeEntity;
import com._team._team.member.domain.enums.AccountStatus;
import com._team._team.member.domain.enums.EmploymentType;
import com._team._team.member.domain.enums.MemberStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id",
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT),
            nullable = false)
    private Company company;

    @Column
    private UUID defaultPositionId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(unique = true)
    private String personalEmail;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    private String phoneNumber;

    @Column(nullable = false)
    @Builder.Default
    private String phonePublicYn = "NO";

    private String emergencyContact;

    private String address;

    private String detailAddress;

    @Column(nullable = false)
    @Builder.Default
    private String addressPublicYn = "NO";

    private String bank;

    private String bankAccount;

    private String profileUrl;

    private String sabun;

    private LocalDate joinDate;

    private String extensionNumber;

    private String telNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus memberStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmploymentType employmentType;

    // 추가
    @Column(nullable = false)
    @Builder.Default
    private Integer loginFailCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private String isFirstLoginYn = "YES";

    @Column(nullable = false)
    @Builder.Default
    private String isEmailVerifiedYn = "NO";

    @Column(nullable = false)
    @Builder.Default
    private String delYn = "NO";

    // 비즈니스 메서드
    public void updateDefaultPosition(UUID defaultPositionId) {
        this.defaultPositionId = defaultPositionId;
    }

    public void increaseLoginFailCount() {
        this.loginFailCount++;
        if (this.loginFailCount >= 5) {
            this.accountStatus = AccountStatus.BLOCKED;
        }
    }

    public void resetLoginFailCount() {
        this.loginFailCount = 0;
    }

    public void verifyEmail() {
        this.isEmailVerifiedYn = "YES";
    }

    public void completeFirstLogin() {
        this.isFirstLoginYn = "NO";
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void unblock() {
        this.accountStatus = AccountStatus.ACTIVE;
        this.loginFailCount = 0;
    }

    public void updateMemberStatus(MemberStatus status) {
        this.memberStatus = status;
    }

    public void delete() {
        this.delYn = "YES";
    }
}