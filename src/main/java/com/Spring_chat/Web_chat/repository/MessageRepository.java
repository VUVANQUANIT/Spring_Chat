package com.Spring_chat.Web_chat.repository;

import com.Spring_chat.Web_chat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;


@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {


}
