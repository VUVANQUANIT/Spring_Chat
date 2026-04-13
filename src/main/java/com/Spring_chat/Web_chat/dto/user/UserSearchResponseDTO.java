package com.Spring_chat.Web_chat.dto.user;

import com.Spring_chat.Web_chat.enums.FriendRelation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchResponseDTO {
    private Long         id;
    private String       username;
    private String       fullName;
    private String       avatarUrl;
    /** Quan hệ bạn bè giữa currentUser và user này — FE dùng để render nút tương ứng */
    private FriendRelation friendRelation;
}
