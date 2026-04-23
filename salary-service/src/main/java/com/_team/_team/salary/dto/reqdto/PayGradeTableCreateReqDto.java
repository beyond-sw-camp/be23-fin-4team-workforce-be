package com._team._team.salary.dto.reqdto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class PayGradeTableCreateReqDto {

    @NotNull
    @Positive
    private Integer step;

    @NotNull
    @PositiveOrZero
    private Long baseSalary;

    @NotNull
    private LocalDate effectiveFrom;

    @Size(max = 200)
    private String description;
}