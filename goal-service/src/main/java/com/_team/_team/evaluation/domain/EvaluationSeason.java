package com._team._team.evaluation.domain;

import com._team._team.domain.BaseTimeEntity;
import com._team._team.evaluation.domain.enums.SeasonStatus;
import com._team._team.evaluation.domain.enums.SeasonType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
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

    @Column(name = "result_publish_date")
    private LocalDate resultPublishDate;

    @Column(name = "schedule_json", columnDefinition = "JSON")
    private String scheduleJson;

    public void activate() {
        this.status = SeasonStatus.ACTIVE;
    }

    public void close() {
        this.status = SeasonStatus.CLOSED;
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
