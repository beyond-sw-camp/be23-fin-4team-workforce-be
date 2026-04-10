package com._team._team.chat.dto.reqdto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatReqDto {

    @NotBlank(message = "질문은 필수입니다.")
    private String question;
}