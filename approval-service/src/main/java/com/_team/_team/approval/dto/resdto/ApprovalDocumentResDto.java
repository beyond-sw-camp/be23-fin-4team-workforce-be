package com._team._team.approval.dto.resdto;

import com._team._team.approval.domain.ApprovalDocument;
import com._team._team.approval.domain.enums.RequestType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ApprovalDocumentResDto {

    private UUID documentId;
    private UUID companyId;
    private String documentName;
    private String formSchema;
    private String isActiveYn;
    private RequestType requestType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ApprovalDocumentResDto fromEntity(ApprovalDocument doc) {
        return ApprovalDocumentResDto.builder()
                .documentId(doc.getDocumentId())
                .companyId(doc.getCompanyId())
                .documentName(doc.getDocumentName())
                .formSchema(doc.getFormSchema())
                .isActiveYn(doc.getIsActiveYn())
                .requestType(doc.getRequestType())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
