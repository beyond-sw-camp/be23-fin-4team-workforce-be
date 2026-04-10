package com._team._team.goal.domain.converter;

import com._team._team.goal.domain.enums.GoalOwnerType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * KPI 템플릿 내 개별 KPI 정의 값 객체.
 * JSON 형태: {"title":"매출","targetValue":100,"unit":"건","ownerId":"uuid","ownerType":"MEMBER"}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KpiDefinition {
    private String title;
    private BigDecimal targetValue;
    private String unit;
    private UUID ownerId;
    private GoalOwnerType ownerType;
}
