package com.Spring_chat.Web_chat.dto.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateMyProfileRequestDTO {

    @Pattern(
            regexp = ".*\\S.*",
            message = "fullName must not be blank"
    )
    @Size(min = 1, max = 100, message = "fullName must be between 1 and 100 characters")
    private String fullName;

    @Size(max = 500, message = "avatarUrl must not exceed 500 characters")
    @Pattern(
            regexp = "^(https?://).+$",
            message = "avatarUrl must be a valid http or https URL"
    )
    private String avatarUrl;
}
