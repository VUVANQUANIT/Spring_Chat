package com.Spring_chat.Web_chat.repository;

import com.Spring_chat.Web_chat.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

}
