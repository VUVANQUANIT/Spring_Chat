package com.Spring_chat.Web_chat.mappers;

import com.Spring_chat.Web_chat.dto.conversations.ConversationDetailDTO;
import com.Spring_chat.Web_chat.dto.conversations.ConversationParticipantDTO;
import com.Spring_chat.Web_chat.dto.conversations.CreateConversationsResponseDTO;
import com.Spring_chat.Web_chat.entity.Conversation;
import com.Spring_chat.Web_chat.entity.ConversationParticipant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ConversationMapper {
    @Mapping(source = "conversation.id", target = "id")
    @Mapping(source = "conversation.type", target = "type")
    @Mapping(source = "conversation.createdAt", target = "createdAt")
    @Mapping(source = "participants", target = "participants")
    CreateConversationsResponseDTO toCreateConversationsResponseDTO(
            Conversation conversation,
            List<ConversationParticipant> participants
    );

    @Mapping(source = "conversation.id", target = "id")
    @Mapping(source = "conversation.type", target = "type")
    @Mapping(source = "conversation.title", target = "title")
    @Mapping(source = "conversation.avatarUrl", target = "avatarUrl")
    @Mapping(source = "conversation.owner.id", target = "ownerId")
    @Mapping(source = "conversation.createdAt", target = "createdAt")
    @Mapping(source = "participants", target = "participants")
    ConversationDetailDTO toConversationDetailDTO(
            Conversation conversation,
            List<ConversationParticipant> participants
    );

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.fullName", target = "fullName")
    @Mapping(source = "user.avatarUrl", target = "avatarUrl")
    @Mapping(source = "leftAt", target = "leftAt")
    @Mapping(target = "isOwner", expression = "java(participant.getConversation() != null && participant.getConversation().getOwner() != null && participant.getConversation().getOwner().getId() != null && participant.getConversation().getOwner().getId().equals(participant.getUser().getId()))")
    ConversationParticipantDTO toConversationParticipantDTO(ConversationParticipant participant);
}
