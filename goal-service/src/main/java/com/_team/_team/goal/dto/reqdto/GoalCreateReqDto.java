package com._team._team.goal.dto.reqdto;

import com._team._team.goal.domain.enums.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalCreateReqDto {

    /** 비어 있으면 측정/단위 기본값으로 목표만 생성합니다. */
    private UUID kpiTemplateId;

    @NotNull(message = "회사 ID는 필수입니다.")
    private UUID companyId;

    private UUID parentGoalId;

    private KpiCycle cycle;

    private RollupPolicy rollupPolicy;

    @NotNull(message = "소유 유형은 필수입니다.")
    private GoalOwnerType ownerType;

    @NotNull(message = "소유자 ID는 필수입니다.")
    private UUID ownerId;

    /**
     * [TEAM 목표 책임자] ownerType == TEAM 일 때 필수, MEMBER 일 때는 무시됨.
     * 서비스 레이어에서 ownerType 과 교차 검증.
     */
    private UUID responsibleMemberId;

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 300)
    private String title;

    @Size(max = 2000)
    private String description;

    @NotNull(message = "시작일은 필수입니다.")
    private LocalDate startDate;

    @NotNull(message = "종료일은 필수입니다.")
    private LocalDate endDate;

    private MeasureType measureType;

    private UnitType unitType;

    @Size(max = 20, message = "단위 표시명은 20자 이하여야 합니다.")
    private String unitLabel;

    private BigDecimal baseline;

    @NotNull(message = "목표값은 필수입니다.")
    @DecimalMin(value = "1", message = "목표 수치는 1 이상이어야 합니다.")
    private BigDecimal targetValue;

    @Min(value = 1, message = "달성률 상한은 1 이상이어야 합니다.")
    private Integer capPct;

    private BigDecimal contributionPct;

    private BigDecimal weightPct;

    @NotNull(message = "공개 범위는 필수입니다.")
    private GoalVisibility visibility;

    @Builder.Default
    private GoalKind goalKind = GoalKind.KR;

    @Builder.Default
    private Boolean autoUpdate = true;

    @Builder.Default
    private Boolean requireApproval = false;

    /**
     * false 이면 DRAFT 로만 저장(일괄 생성 후 번들 승인 등).
     * 단건 생성에서는 기본 true 로 즉시 승인 플로 또는 활성화를 진행합니다.
     */
    @Builder.Default
    private Boolean activateImmediately = true;

    private UUID approverId;

    private List<UUID> visibleTeamIds;

    private List<UUID> memberIds;
}
