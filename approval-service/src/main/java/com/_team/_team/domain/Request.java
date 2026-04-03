package com._team._team.domain;

import com._team._team.domain.enums.RequestStatus;
import com._team._team.domain.enums.RequestType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@Entity
public class Request extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false)
    private UUID requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private ApprovalDocument approvalDocument;

    @Column(nullable = false)
    private UUID memberId; // 요청자ID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestType requestType; // VACATION, ATTENDANCE, HR_MOVEMENT, SALARY, GENERAL,CERTIFICATE

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "json")
    private String contentJson; // 실제 입력된 값 (key: value 형태)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    private LocalDateTime startDateTime;

    private LocalDateTime endDateTime;

    @Builder.Default
    @Column(nullable = false)
    private String delYn = "N";

    @Column(columnDefinition = "TEXT")
    private String cancelReason;

    public void updateStatus(RequestStatus status) {
        this.status = status;
    }

    public void cancel(String reason) {
        this.status = RequestStatus.REJECTED;
        this.cancelReason = reason;
    }

    public void updateContentJson(String contentJson) {
        this.contentJson = contentJson;
    }

    public void updateStartDateTime(LocalDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public void updateEndDateTime(LocalDateTime endDateTime) {
        this.endDateTime = endDateTime;
    }
}
