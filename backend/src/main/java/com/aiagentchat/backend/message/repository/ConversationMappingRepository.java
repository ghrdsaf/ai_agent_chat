package com.aiagentchat.backend.message.repository;

import com.aiagentchat.backend.message.entity.ConversationMappingEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationMappingRepository extends JpaRepository<ConversationMappingEntity, Long> {

    Optional<ConversationMappingEntity> findByChatwootConversationId(Long chatwootConversationId);
}

