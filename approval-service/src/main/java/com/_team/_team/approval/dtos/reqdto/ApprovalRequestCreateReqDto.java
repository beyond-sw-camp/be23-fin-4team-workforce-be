package com._team._team.approval.dtos.reqdto;

import com._team._team.approval.domain.enums.RequestStatus;
import com._team._team.approval.domain.enums.ViewerType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ApprovalRequestCreateReqDto {

    @NotNull(message = "문서 ID는 필수입니다.")
    private UUID documentId;

    @NotNull(message = "내용은 필수입니다.")
    private String contentJson;

    @NotNull(message = "요청 상태는 필수입니다.")
    private RequestStatus requestStatus; // DRAFT or WAIT

    @Valid
    private List<ApprovalLineItem> approvalLines;

    private List<ViewerItem> viewers;

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class ApprovalLineItem {

        @NotNull(message = "결재 순서는 필수입니다.")
        private Integer stepOrder;

        @NotNull(message = "결재자 직위 ID는 필수입니다.")
        private UUID memberPositionId;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class ViewerItem {

        @NotNull(message = "참조/공람자 ID는 필수입니다.")
        private UUID memberId;

        @NotNull(message = "참조/공람 구분은 필수입니다.")
        private ViewerType viewerType; // CC or CIRCULATION
    }

}
