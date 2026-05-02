package com.Spring_chat.Web_chat.repository;

import com.Spring_chat.Web_chat.entity.MessageHidden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MessageHiddenRepository extends JpaRepository<MessageHidden, Long> {
    Optional<MessageHidden> findByMessageIdAndUserId(Long messageId, Long userId);
    boolean existsByMessageIdAndUserId(Long messageId, Long userId);
}
