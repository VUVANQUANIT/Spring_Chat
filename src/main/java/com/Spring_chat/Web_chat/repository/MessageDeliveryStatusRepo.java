package com.Spring_chat.Web_chat.repository;

import com.Spring_chat.Web_chat.entity.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageDeliveryStatusRepo extends JpaRepository<MessageStatus, Long> {
    List<MessageStatus> findAllByMessage_IdIn(List<Long> messageIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE MessageStatus ms
            SET ms.status = com.Spring_chat.Web_chat.enums.MessageDeliveryStatus.SEEN,
                ms.updatedAt = :updatedAt
            WHERE ms.user.id = :userId
              AND ms.status <> com.Spring_chat.Web_chat.enums.MessageDeliveryStatus.SEEN
              AND ms.message.conversation.id = :conversationId
              AND ms.message.id <= :lastReadMessageId
            """)
    int updateStatusToSeenForUserAndConversation(
            @Param("userId") Long userId,
            @Param("conversationId") Long conversationId,
            @Param("lastReadMessageId") Long lastReadMessageId,
            @Param("updatedAt") java.time.Instant updatedAt
    );

    @Query("""
            SELECT COUNT(ms)
            FROM MessageStatus ms
            WHERE ms.user.id = :userId
              AND ms.message.conversation.id = :conversationId
              AND ms.status <> com.Spring_chat.Web_chat.enums.MessageDeliveryStatus.SEEN
              AND ms.message.isDeleted = false
            """)
    long countUnreadMessages(@Param("userId") Long userId, @Param("conversationId") Long conversationId);
}

