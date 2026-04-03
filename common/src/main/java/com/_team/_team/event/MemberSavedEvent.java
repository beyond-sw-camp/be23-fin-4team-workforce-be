package com._team._team.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberSavedEvent {
    private UUID memberId;
    private UUID companyId;
    private String name;
    private List<OrganizationSavedEvent> organizationList;
    private List<String> titleName;
    private String phoneNumber;
    private String email;
    private String memberStatus;
    private String position;
}
