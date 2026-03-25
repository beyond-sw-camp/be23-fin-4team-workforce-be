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

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(columnDefinition = "BINARY(16)")
    private UUID defaultPositionId;

    @Column(nullable = false, unique = true)
    private String email;

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
    private AccountStatus accountStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmploymentType employmentType;

    @Column(nullable = false)
    @Builder.Default
    private String delYn = "NO";

    public void deleteUpdate() {
        this.delYn = "YES";
    }
}