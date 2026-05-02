package com.Spring_chat.Web_chat.repository;

import com.Spring_chat.Web_chat.dto.conversations.ConversationRowProjection;
import com.Spring_chat.Web_chat.entity.ConversationParticipant;
import com.Spring_chat.Web_chat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {
    List<ConversationParticipant> findByConversation_Id(Long conversationId);
    List<ConversationParticipant> findAllByConversation_IdAndLeftAtIsNull(Long conversationId);
    List<ConversationParticipant> findByUser_Id(Long userId);
    List<ConversationParticipant> findAllByConversation_IdOrderByJoinedAtAsc(Long conversationId);
    boolean existsByConversation_IdAndUser_IdAndLeftAtIsNull(Long conversationId, Long userId);

    /**
     * Lấy danh sách conversations của user theo inbox order (cursor-based pagination).
     *
     * Cursor là ISO-8601 timestamp của sort-time (COALESCE(lastMessage.createdAt, conversation.createdAt))
     * của item cuối cùng ở trang trước. Trả về :limit + 1 rows để caller tự detect hasMore.
     *
     * Rules:
     * - Chỉ trả conversations mà user là participant.
     * - Nếu leftAt != null: lastMessage và unreadCount chỉ tính message có createdAt <= leftAt.
     * - Sắp xếp: COALESCE(m.created_at, c.created_at) DESC, c.id DESC.
     * - otherParticipant chỉ có giá trị với PRIVATE; GROUP trả NULL.
     */
    @Query(value = """
            SELECT
                c.id                                                    AS "id",
                c.type                                                  AS "type",
                c.title                                                 AS "title",
                c.avatar_url                                            AS "avatarUrl",
                c.created_at                                            AS "conversationCreatedAt",
                m.id                                                    AS "lastMessageId",
                m.content                                               AS "lastMessageContent",
                m.type                                                  AS "lastMessageType",
                m.sender_id                                             AS "lastMessageSenderId",
                u.username                                              AS "senderUsername",
                m.created_at                                            AS "lastMessageCreatedAt",
                m.is_deleted                                            AS "lastMessageIsDeleted",
                (
                    SELECT COUNT(*)
                    FROM   messages m3
                    WHERE  m3.conversation_id = c.id
                      AND  (
                               CASE
                                   WHEN cp.last_read_message_id IS NULL THEN TRUE
                                   ELSE m3.created_at > (
                                       SELECT m4.created_at FROM messages m4
                                       WHERE  m4.id = cp.last_read_message_id
                                   )
                               END
                           )
                      AND  (
                               cp.left_at IS NULL
                               OR (cp.left_at IS NOT NULL AND m3.created_at <= cp.left_at)
                           )
                )                                                       AS "unreadCount",
                CASE WHEN c.type = 'PRIVATE' THEN u2.id          ELSE NULL END AS "otherUserId",
                CASE WHEN c.type = 'PRIVATE' THEN u2.username    ELSE NULL END AS "otherUsername",
                CASE WHEN c.type = 'PRIVATE' THEN u2.avatar_url  ELSE NULL END AS "otherAvatarUrl",
                CASE
                    WHEN c.type = 'PRIVATE' AND u2.last_seen > :onlineThreshold THEN TRUE
                    WHEN c.type = 'PRIVATE' THEN FALSE
                    ELSE NULL
                END                                                     AS "isOnline"
            FROM  conversation_participants cp
            JOIN  conversations c ON c.id = cp.conversation_id
            LEFT JOIN messages m ON m.id = (
                SELECT id FROM messages m2
                WHERE  m2.conversation_id = c.id
                  AND  (
                           (cp.left_at IS NULL)
                           OR (cp.left_at IS NOT NULL AND m2.created_at <= cp.left_at)
                       )
                ORDER BY m2.created_at DESC
                LIMIT 1
            )
            LEFT JOIN users u   ON u.id = m.sender_id
            LEFT JOIN conversation_participants cp2
                   ON cp2.conversation_id = c.id
                  AND cp2.user_id != cp.user_id
                  AND c.type = 'PRIVATE'
            LEFT JOIN users u2  ON u2.id = cp2.user_id
            WHERE cp.user_id = :userId
              AND (
                      :cursor IS NULL
                      OR COALESCE(m.created_at, c.created_at) < :cursor
                  )
            ORDER BY COALESCE(m.created_at, c.created_at) DESC, c.id DESC
            LIMIT :limit
            """, nativeQuery = true)
            List<ConversationRowProjection> findUserConversations(
            @Param("userId") Long   userId,
            @Param("cursor") OffsetDateTime cursor,
            @Param("limit")  int    limit,
            @Param("onlineThreshold") Instant onlineThreshold
            );

    ConversationParticipant findByConversation_IdAndUser(Long conversationId, User user);
    java.util.Optional<ConversationParticipant> findByConversation_IdAndUser_Id(Long conversationId, Long userId);
    java.util.Optional<ConversationParticipant> findByConversation_IdAndUser_IdAndLeftAtIsNull(Long conversationId, Long userId);

    java.util.Optional<ConversationParticipant> findFirstByConversation_IdAndLeftAtIsNullOrderByJoinedAtAsc(Long conversationId);

    java.util.Optional<ConversationParticipant> findFirstByConversation_IdAndUser_IdNotAndLeftAtIsNullOrderByJoinedAtAsc(Long conversationId, Long userId);

    boolean existsByConversation_IdAndUser_IdNotAndLeftAtIsNull(Long conversationId, Long userId);
}
