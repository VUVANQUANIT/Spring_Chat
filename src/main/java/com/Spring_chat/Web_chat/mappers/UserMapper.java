package com.Spring_chat.Web_chat.mappers;

import com.Spring_chat.Web_chat.dto.user.MyProfileUserDTO;
import com.Spring_chat.Web_chat.dto.user.ProfileUserDTO;
import com.Spring_chat.Web_chat.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {
    ProfileUserDTO userToUserDTO(User user);
    MyProfileUserDTO userToMyUserDTO(User user);
}
