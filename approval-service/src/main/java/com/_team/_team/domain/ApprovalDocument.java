package com._team._team.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@Entity
public class ApprovalDocument extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false)
    private UUID documentId;

    @Column(nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private String documentName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String formSchema;

    @Builder.Default
    @Column(nullable = false)
    private String isActiveYn="Y"; //문서 사용 여부


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestType requestType;

    @Builder.Default
    @Column(nullable = false)
    private String autoApproveYn="N"; // 자동승인여부


}
