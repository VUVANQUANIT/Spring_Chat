package com.Spring_chat.Web_chat.repository;

import com.Spring_chat.Web_chat.dto.message.MessageRowProjection;
import com.Spring_chat.Web_chat.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;


@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    @Query(value = """
        SELECT 
            m.id, m.conversation_id as conversationId, m.content, m.type, 
            m.reply_to_id as replyToId, m.is_deleted as isDeleted, 
            m.is_edited as isEdited, m.edited_at as editedAt, m.created_at as createdAt,
            u.id as senderId, u.username as senderUsername, u.avatar_url as senderAvatar,
            ms.status as myStatus
        FROM messages m
        INNER JOIN users u ON m.sender_id = u.id
        LEFT JOIN message_statuses ms ON ms.message_id = m.id AND ms.user_id = :userId
        WHERE m.conversation_id = :convId
          AND (:beforeCreatedAt IS NULL OR (
              m.created_at < :beforeCreatedAt
              OR (m.created_at = :beforeCreatedAt AND m.id < :beforeId)
          ))
          AND NOT EXISTS (SELECT 1 FROM message_hidden mh WHERE mh.message_id = m.id AND mh.user_id = :userId)
        ORDER BY m.created_at DESC, m.id DESC
        """, nativeQuery = true)
    List<MessageRowProjection> findMessagesByConversation(
            @Param("convId") Long convId,
            @Param("userId") Long userId,
            @Param("beforeCreatedAt") Instant beforeCreatedAt,
            @Param("beforeId") Long beforeId,
            Pageable pageable
    );

    @Query("SELECT m.createdAt FROM Message m WHERE m.id = :id")
    Instant findCreatedAtById(@Param("id") Long id);

    Optional<Message> findFirstByConversation_IdAndSender_IdAndClientMessageIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            Long conversationId,
            Long senderId,
            String clientMessageId,
            Instant threshold
    );

    Optional<Message> findByIdAndConversation_Id(Long id, Long conversationId);
}
