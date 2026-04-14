package com._team._team.evaluation.domain;

import com._team._team.dto.BusinessException;
import com._team._team.domain.BaseTimeEntity;
import com._team._team.evaluation.domain.enums.SeasonPhase;
import com._team._team.evaluation.domain.enums.SeasonStatus;
import com._team._team.evaluation.domain.enums.SeasonType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.http.HttpStatus;

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
     * ACTIVE 상태 내에서의 세부 진행 페이즈.
     * DRAFT 일 때는 NOT_STARTED, CLOSED 이후에도 마지막 phase 가 유지됩니다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "phase", nullable = false, length = 32)
    @Builder.Default
    private SeasonPhase phase = SeasonPhase.NOT_STARTED;

    @Column(name = "phase_updated_at")
    private LocalDateTime phaseUpdatedAt;

    @Column(name = "result_publish_date")
    private LocalDate resultPublishDate;

    @Column(name = "schedule_json", columnDefinition = "JSON")
    private String scheduleJson;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    public void activate() {
        this.status = SeasonStatus.ACTIVE;
        if (this.phase == null) {
            this.phase = SeasonPhase.NOT_STARTED;
        }
        this.phaseUpdatedAt = LocalDateTime.now();
    }

    public void close() {
        this.status = SeasonStatus.CLOSED;
    }

    /**
     * 상태 기계 기반의 phase 전이. 허용된 선형 순서대로만 이동할 수 있습니다.
     */
    public SeasonPhase transitionTo(SeasonPhase next) {
        if (this.status != SeasonStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "시즌이 ACTIVE 상태가 아닙니다. phase 전이를 수행할 수 없습니다.");
        }
        if (next == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "전이 대상 phase 가 지정되지 않았습니다.");
        }
        if (!isAllowedNext(this.phase, next)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "phase 전이가 허용되지 않습니다: " + this.phase + " → " + next);
        }
        SeasonPhase previous = this.phase;
        this.phase = next;
        this.phaseUpdatedAt = LocalDateTime.now();
        return previous;
    }

    private static boolean isAllowedNext(SeasonPhase from, SeasonPhase to) {
        if (from == null) from = SeasonPhase.NOT_STARTED;
        return switch (from) {
            case NOT_STARTED -> to == SeasonPhase.SELF_EVAL;
            case SELF_EVAL -> to == SeasonPhase.PEER_EVAL;
            case PEER_EVAL -> to == SeasonPhase.UPWARD_EVAL;
            case UPWARD_EVAL -> to == SeasonPhase.DOWNWARD_EVAL;
            case DOWNWARD_EVAL -> to == SeasonPhase.CALIBRATION;
            case CALIBRATION -> to == SeasonPhase.CONFIRMED;
            case CONFIRMED -> to == SeasonPhase.PUBLISHED;
            case PUBLISHED -> false;
        };
    }

    /**
     * 현재 phase 가 구성원이 응답 입력(저장/제출) 가능한 단계인지 여부.
     */
    public boolean isResponseEditablePhase() {
        return this.phase == SeasonPhase.SELF_EVAL
                || this.phase == SeasonPhase.PEER_EVAL
                || this.phase == SeasonPhase.UPWARD_EVAL
                || this.phase == SeasonPhase.DOWNWARD_EVAL;
    }

    /**
     * 캘리브레이션 조정이 가능한 phase 인지 여부.
     */
    public boolean isCalibrationPhase() {
        return this.phase == SeasonPhase.CALIBRATION;
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
