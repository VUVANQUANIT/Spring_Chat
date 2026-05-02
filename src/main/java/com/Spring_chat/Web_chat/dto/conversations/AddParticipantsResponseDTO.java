package com.Spring_chat.Web_chat.dto.conversations;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddParticipantsResponseDTO {
    private Long[] addedUserIds;
}
