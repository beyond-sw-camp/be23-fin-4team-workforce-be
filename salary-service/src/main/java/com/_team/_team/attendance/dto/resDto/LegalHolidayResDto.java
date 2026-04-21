package com._team._team.attendance.dto.resDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class LegalHolidayResDto {

    private LocalDate holidayDate;
    private String holidayName;
    private int year;
}