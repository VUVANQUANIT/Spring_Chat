package com.Spring_chat.Web_chat.repository;

import com.Spring_chat.Web_chat.entity.ConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {
    List<ConversationParticipant> findByConversation_Id(Long conversationId);
}
