package com.aiagentchat.backend.message.repository;

import com.aiagentchat.backend.message.entity.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {

    boolean existsByFingerprint(String fingerprint);
}

