package com.Spring_chat.Web_chat.repository;

import com.Spring_chat.Web_chat.entity.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageDeliveryStatusRepo extends JpaRepository<MessageStatus, Long> {
}
