package com.Spring_chat.Web_chat.repository;

import com.Spring_chat.Web_chat.entity.ConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {
}
