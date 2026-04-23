package com._team._team.attendance.dto.resDto;

import com._team._team.attendance.domain.CompanyIpWhitelist;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class CompanyIpWhitelistResDto {

    private UUID companyIpWhitelistId;
    private String cidr;
    private String label;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CompanyIpWhitelistResDto fromEntity(CompanyIpWhitelist e) {
        return CompanyIpWhitelistResDto.builder()
                .companyIpWhitelistId(e.getCompanyIpWhitelistId())
                .cidr(e.getCidr())
                .label(e.getLabel())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}