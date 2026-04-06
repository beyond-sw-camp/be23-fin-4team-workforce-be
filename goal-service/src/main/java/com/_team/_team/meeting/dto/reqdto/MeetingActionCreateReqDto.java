package com._team._team.meeting.dto.reqdto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingActionCreateReqDto {

    private UUID assigneeId;

    @NotBlank(message = "액션 설명은 필수입니다.")
    @Size(max = 500)
    private String description;

    private LocalDate dueDate;
}
