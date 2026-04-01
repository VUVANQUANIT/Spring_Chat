package com.Spring_chat.Spring_chat.mappers;

import com.Spring_chat.Spring_chat.dto.friendship.FriendRequestResponseDTO;
import com.Spring_chat.Spring_chat.entity.Friendship;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface FriendShipMapper {
    @Mapping(source = "addressee.id", target = "addresseeId")
    @Mapping(source = "addressee.username", target = "addresseeUsername")
    @Mapping(source = "addressee.avatarUrl", target = "addresseeAvatarUrl")
    FriendRequestResponseDTO toFriendRequestResponseDTO(Friendship friendship);
}
