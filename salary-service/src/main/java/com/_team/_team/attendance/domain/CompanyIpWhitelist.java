package com._team._team.attendance.domain;

import com._team._team.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 회사 허용 IP 리스트
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@Entity
@Table(
        indexes = {
                @Index(name = "idx_company_ip_whitelist_company", columnList = "companyId, delYn")
        }
)
public class CompanyIpWhitelist extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID companyIpWhitelistId;

    @Column(nullable = false)
    private UUID companyId;

    /** CIDR 형식, 예: "192.168.1.0/24" 또는 "203.0.113.5/32" */
    @Column(nullable = false, length = 50)
    private String cidr;

    /** 라벨, 예: "본사 1층", "제2사옥" */
    @Column(length = 100)
    private String label;

    @Column(nullable = false, length = 1)
    @Builder.Default
    private String delYn = "N";

    public void update(String cidr, String label) {
        this.cidr = cidr;
        this.label = label;
    }

    public void delete() {
        this.delYn = "Y";
    }
}