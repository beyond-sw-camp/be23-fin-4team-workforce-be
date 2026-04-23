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

    // [T-1] phase / phase_updated_at 컬럼은 더 이상 엔티티에 매핑하지 않음.
    // DB 마이그레이션에서 DROP:
    //   ALTER TABLE evaluation_season DROP COLUMN phase;
    //   ALTER TABLE evaluation_season DROP COLUMN phase_updated_at;
    // 기존 데이터는 status + results_published_at 로 모두 표현됨.

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