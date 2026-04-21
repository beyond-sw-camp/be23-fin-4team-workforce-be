package com._team._team.evaluation.domain;

import com._team._team.domain.BaseTimeEntity;
import com._team._team.evaluation.domain.enums.SeasonStatus;
import com._team._team.evaluation.domain.enums.SeasonType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "evaluation_season")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EvaluationSeason extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "season_id")
    private UUID seasonId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private SeasonType type;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private SeasonStatus status = SeasonStatus.DRAFT;

    /**
     * [DEPRECATED] 세부 phase 기반 워크플로는 제거되었습니다.
     * 이제 시즌은 status(DRAFT/ACTIVE/CLOSED) 와 resultsPublishedAt 으로 관리됩니다.
     *
     * 기존 데이터 보존 + 롤백 안전을 위해 컬럼은 유지하며, 새로 생성되는 시즌에는
     * 항상 'LEGACY_DEPRECATED' 값을 저장합니다. 서비스 로직은 이 필드를 참조하지 않습니다.
     *
     * TODO: 2-3 릴리즈 후 DB 컬럼까지 DROP.
     */
    @Column(name = "phase", nullable = false, length = 32)
    @Builder.Default
    private String phase = "LEGACY_DEPRECATED";

    @Column(name = "phase_updated_at")
    private LocalDateTime phaseUpdatedAt;

    @Column(name = "result_publish_date")
    private LocalDate resultPublishDate;

    /**
     * 결과가 피평가자에게 공개된 시각. null 이면 비공개 상태.
     * 관리자의 "결과 공개" 액션 또는 resultPublishDate 에 의해 스케줄러가 세팅합니다.
     */
    @Column(name = "results_published_at")
    private LocalDateTime resultsPublishedAt;

    @Column(name = "schedule_json", columnDefinition = "JSON")
    private String scheduleJson;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    public void activate() {
        this.status = SeasonStatus.ACTIVE;
        this.phaseUpdatedAt = LocalDateTime.now();
    }

    public void close() {
        this.status = SeasonStatus.CLOSED;
    }

    /**
     * 결과 공개 (피평가자에게 노출). 이미 공개된 시즌에 재호출은 무시.
     */
    public void markResultsPublished() {
        if (this.resultsPublishedAt == null) {
            this.resultsPublishedAt = LocalDateTime.now();
        }
    }

    public boolean isResultsPublished() {
        return this.resultsPublishedAt != null;
    }

    /**
     * 구성원이 응답 입력(저장/제출) 가능한 상태인지 여부.
     * ACTIVE 이면서 아직 결과가 공개되지 않은 경우에만 허용.
     */
    public boolean isResponseEditable() {
        return this.status == SeasonStatus.ACTIVE && this.resultsPublishedAt == null;
    }

    /**
     * 캘리브레이션 조정이 가능한 상태인지 여부.
     * ACTIVE 이면서 결과 공개 전에만 조정 가능.
     */
    public boolean canCalibrate() {
        return this.status == SeasonStatus.ACTIVE && this.resultsPublishedAt == null;
    }

    public void update(String name, SeasonType type, LocalDate startDate, LocalDate endDate,
                       LocalDate resultPublishDate, String scheduleJson) {
        this.name = name;
        this.type = type;
        this.startDate = startDate;
        this.endDate = endDate;
        this.resultPublishDate = resultPublishDate;
        this.scheduleJson = scheduleJson;
    }
}
