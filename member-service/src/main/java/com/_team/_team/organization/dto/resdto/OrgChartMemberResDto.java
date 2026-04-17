package com._team._team.organization.dto.resdto;

import com._team._team.member.domain.Member;
import com._team._team.member.domain.MemberPosition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrgChartMemberResDto {

    private UUID memberId;
    private String name;
    private String jobGradeName;

    public static OrgChartMemberResDto fromEntity(Member member, MemberPosition position) {
        return OrgChartMemberResDto.builder()
                .memberId(member.getMemberId())
                .name(member.getName())
                .jobGradeName(position.getJobGrade().getName())
                .build();
    }
}
