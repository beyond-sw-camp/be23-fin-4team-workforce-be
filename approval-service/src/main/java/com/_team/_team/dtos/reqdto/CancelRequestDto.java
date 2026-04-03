package com._team._team.dtos.reqdto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class CancelRequestDto {
    @NotBlank(message = "회수 사유는 필수입니다")
    private String cancelReason;
}
