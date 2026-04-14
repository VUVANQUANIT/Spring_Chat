package com.Spring_chat.Web_chat.repository;

import com.Spring_chat.Web_chat.dto.message.MessageRowProjection;
import com.Spring_chat.Web_chat.entity.Conversation;
import com.Spring_chat.Web_chat.entity.Message;
import com.Spring_chat.Web_chat.entity.User;
import com.Spring_chat.Web_chat.enums.ConversationType;
import com.Spring_chat.Web_chat.enums.MessageType;
import com.Spring_chat.Web_chat.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("dev")
class MessageRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MessageRepository messageRepository;

    private User sender;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        // Setup User
        sender = User.builder()
                .username("test_sender")
                .email("test@example.com")
                .passwordHash("hash")
                .status(UserStatus.ACTIVE)
                .build();
        entityManager.persist(sender);
        entityManager.flush();

        // Setup Conversation
        conversation = Conversation.builder()
                .type(ConversationType.GROUP)
                .title("Test Group")
                .build();
        entityManager.persist(conversation);
        entityManager.flush();
    }

    @Test
    @DisplayName("findMessagesByConversation nên sắp xếp theo createdAt DESC và id DESC")
    void findMessagesByConversation_ShouldSortByCreatedAtDescAndIdDesc() {
        Instant baseTime = Instant.now().minus(1, ChronoUnit.DAYS);

        // Tạo 3 tin nhắn:
        // m1: created_at = baseTime
        Message m1 = createMessage(baseTime, "Msg 1");
        
        // m2: created_at = baseTime + 1 giờ (Mới hơn m1)
        Message m2 = createMessage(baseTime.plus(1, ChronoUnit.HOURS), "Msg 2");
        
        // m3: created_at = baseTime + 1 giờ (Cùng thời gian m2, nhưng ID lớn hơn vì insert sau)
        Message m3 = createMessage(baseTime.plus(1, ChronoUnit.HOURS), "Msg 3");

        entityManager.flush();
        entityManager.clear();

        // Lấy danh sách tin nhắn
        List<MessageRowProjection> messages = messageRepository.findMessagesByConversation(
                conversation.getId(), sender.getId(), null, 10);

        assertThat(messages).hasSize(3);
        // Thứ tự mong đợi: m3 (mới nhất, id lớn hơn), m2 (mới nhất, id nhỏ hơn m3), m1 (cũ nhất)
        assertThat(messages.get(0).getId()).isEqualTo(m3.getId());
        assertThat(messages.get(1).getId()).isEqualTo(m2.getId());
        assertThat(messages.get(2).getId()).isEqualTo(m1.getId());
    }

    @Test
    @DisplayName("Phân trang (Cursor) với beforeId hoạt động đúng")
    void findMessagesByConversation_CursorPaginationShouldWork() {
        Instant baseTime = Instant.now().minus(1, ChronoUnit.DAYS);

        Message m1 = createMessage(baseTime, "Msg 1");
        Message m2 = createMessage(baseTime.plus(1, ChronoUnit.HOURS), "Msg 2");
        Message m3 = createMessage(baseTime.plus(2, ChronoUnit.HOURS), "Msg 3");
        Message m4 = createMessage(baseTime.plus(3, ChronoUnit.HOURS), "Msg 4");

        entityManager.flush();
        entityManager.clear();

        // Lấy tin nhắn cũ hơn m3 (beforeId = m3.getId())
        List<MessageRowProjection> messages = messageRepository.findMessagesByConversation(
                conversation.getId(), sender.getId(), m3.getId(), 2);

        // Tin nhắn trả về phải là m2 và m1, bỏ qua m4 và m3
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).getId()).isEqualTo(m2.getId());
        assertThat(messages.get(1).getId()).isEqualTo(m1.getId());
    }

    private Message createMessage(Instant createdAt, String content) {
        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(content)
                .type(MessageType.TEXT)
                .isDeleted(false)
                .isEdited(false)
                .build();
        
        entityManager.persist(message);
        
        entityManager.createNativeQuery("UPDATE messages SET created_at = :time WHERE id = :id")
                .setParameter("time", createdAt)
                .setParameter("id", message.getId())
                .executeUpdate();
        
        return message;
    }
}
