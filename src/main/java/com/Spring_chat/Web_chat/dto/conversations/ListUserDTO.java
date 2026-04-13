package com.Spring_chat.Web_chat.dto.conversations;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;


@Data
public class ListUserDTO {
    private Long[] userIds;
    @JsonProperty("userIds") // Chỉ dùng khi nhận Request
    public void setUserIds(Long[] userIds) {
        this.userIds = userIds;
    }

    @JsonProperty("addedUserIds") // Chỉ dùng khi trả Response
    public Long[] getUserIds() {
        return userIds;
    }
}
