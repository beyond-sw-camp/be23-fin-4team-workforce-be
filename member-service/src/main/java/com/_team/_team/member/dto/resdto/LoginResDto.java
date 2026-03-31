package com._team._team.member.dto.resdto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginResDto {
    private String accessToken;
    private String name;
    private UUID memberId;
    private UUID memberPositionId;
    private String isFirstLoginYn;
    private String isEmailVerifiedYn;
}
