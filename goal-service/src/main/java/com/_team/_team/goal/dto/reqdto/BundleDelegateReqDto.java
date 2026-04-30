package com._team._team.goal.dto.reqdto;

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
public class BundleDelegateReqDto {

    @NotNull(message = "위임 받을 승인자 ID 필수")
    private UUID delegateApproverId;
}
