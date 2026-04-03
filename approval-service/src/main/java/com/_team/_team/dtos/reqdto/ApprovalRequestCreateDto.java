package com._team._team.dtos.reqdto;

import com._team._team.domain.enums.RequestType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ApprovalRequestCreateDto {
    @NotNull(message = "문서 양식 ID는 필수입니다")
    private UUID documentId;

    @NotNull(message = "요청 유형은 필수입니다")
    private RequestType requestType;

    @NotNull(message = "요청 내용은 필수입니다")
    private Object contentJson;

    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;

    @Builder.Default
    private boolean draft = false;

    private List<ApprovalLineItemDto> approvalLines;

    private List<ViewerItemDto> viewers;


}



