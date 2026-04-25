package com._team._team.attendance.dto.reqDto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CompanyIpWhitelistUpdateReqDto {

    @NotBlank
    @Pattern(
            regexp = "^((25[0-5]|2[0-4]\\d|[01]?\\d?\\d)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d?\\d)(/(3[0-2]|[12]?\\d))?$",
            message = "CIDR 형식이 올바르지 않습니다."
    )
    @Size(max = 50)
    private String cidr;

    @Size(max = 100)
    private String label;
}