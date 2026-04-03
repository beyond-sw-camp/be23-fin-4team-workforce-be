package com._team._team.dtos.reqdto;

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
public class ViewerItemDto {

    @NotNull
    private UUID memberId;

    @NotNull
    private String viewerType;  // "CC" / "CIRCULATION"
}
