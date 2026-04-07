package com.Spring_chat.Web_chat.repository;

import com.Spring_chat.Web_chat.entity.Conversation;
import com.Spring_chat.Web_chat.enums.ConversationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    @Query("""
        select c from Conversation c
        where c.type = :type
          and c.id in (
              select cp.conversation.id
              from ConversationParticipant cp
              where cp.user.id in (:userA, :userB)
              group by cp.conversation.id
              having count(distinct cp.user.id) = 2
          )
          and (select count(cp2.id) from ConversationParticipant cp2 where cp2.conversation.id = c.id) = 2
        """)
    Optional<Conversation> findPrivateBetween(
            @Param("type") ConversationType type,
            @Param("userA") Long userA,
            @Param("userB") Long userB
    );

}
