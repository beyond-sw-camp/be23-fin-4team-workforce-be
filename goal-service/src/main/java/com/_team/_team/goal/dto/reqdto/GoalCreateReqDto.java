package com._team._team.goal.dto.reqdto;

import com._team._team.goal.domain.enums.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalCreateReqDto {

    @NotNull(message = "KPI 템플릿 ID는 필수입니다.")
    private UUID kpiTemplateId;

    @NotNull(message = "회사 ID는 필수입니다.")
    private UUID companyId;

    private UUID parentGoalId;

    @NotNull(message = "소유 유형은 필수입니다.")
    private GoalOwnerType ownerType;

    @NotNull(message = "소유자 ID는 필수입니다.")
    private UUID ownerId;

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 300)
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    @Size(max = 300)
    private String description;

    @NotNull(message = "시작일은 필수입니다.")
    private LocalDate startDate;

    @NotNull(message = "종료일은 필수입니다.")
    private LocalDate endDate;

    @NotNull(message = "측정 유형은 필수입니다.")
    private MeasureType measureType;

    @NotNull(message = "단위 유형은 필수입니다.")
    private UnitType unitType;

    private String unitLabel;

    @NotNull(message = "기준값은 필수입니다.")
    private BigDecimal baseline;

    @NotNull(message = "목표값은 필수입니다.")
    private BigDecimal targetValue;

    private int capPct;

    private BigDecimal contributionPct;

    private BigDecimal weightPct;

    @NotNull(message = "공개 범위는 필수입니다.")
    private GoalVisibility visibility;
}
