package com._team._team.attendance.dto.reqDto;

import jakarta.validation.constraints.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class CompanyIpWhitelistCreateReqDto {

    @NotBlank(message = "CIDR 은 필수입니다.")
    @Pattern(
            regexp = "^((25[0-5]|2[0-4]\\d|[01]?\\d?\\d)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d?\\d)(/(3[0-2]|[12]?\\d))?$",
            message = "CIDR 형식이 올바르지 않습니다. 예: 192.168.1.0/24"
    )
    @Size(max = 50)
    private String cidr;

    @Size(max = 100)
    private String label;
}