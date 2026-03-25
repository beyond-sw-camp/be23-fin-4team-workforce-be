package com._team._team.company.domain;

import com._team._team.company.domain.enums.CompanyStatus;
import com._team._team.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.apache.kafka.common.protocol.types.Field;

import java.util.UUID;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class Company extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID companyId;
    @Column(nullable = false)
    private String companyName;
    private String logoUrl;
    private String businessNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyStatus status;
}
