package com._team._team.dtos.reqdto;

import com._team._team.domain.enums.RequestType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;


@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ApprovalDocumentCreateDto {

    @NotNull(message = "회사 ID는 필수입니다")
    private UUID companyId; // 추가

    @NotBlank(message = "문서명은 필수입니다")
    private String documentName;

    @NotNull(message = "문서 유형은 필수입니다")
    private RequestType requestType;

    @NotNull(message = "폼 스키마는 필수입니다")
    private Object formSchema; // Vue에서 JSON 객체로 전달

    @Builder.Default
    private String autoApproveYn = "N";
}
