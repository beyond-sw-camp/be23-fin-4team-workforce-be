package com._team._team.dtos.reqdto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalLineItemDto {
    @NotNull
    private UUID policyLineId;

    @NotNull
    private UUID memberPositionId;

    @NotNull
    private Integer stepOrder;
}
