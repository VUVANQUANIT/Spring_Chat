package com.Spring_chat.Web_chat.repository;

import com.Spring_chat.Web_chat.entity.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageDeliveryStatusRepo extends JpaRepository<MessageStatus, Long> {
    List<MessageStatus> findAllByMessage_IdIn(List<Long> messageIds);
}

