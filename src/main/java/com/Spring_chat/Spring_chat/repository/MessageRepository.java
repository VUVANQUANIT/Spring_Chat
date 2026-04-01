package com.Spring_chat.Spring_chat.repository;

import com.Spring_chat.Spring_chat.entity.ConversationParticipant;
import com.Spring_chat.Spring_chat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {
}
