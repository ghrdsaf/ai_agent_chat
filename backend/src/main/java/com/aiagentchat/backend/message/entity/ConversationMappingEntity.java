package com.aiagentchat.backend.message.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "conversation_mapping")
public class ConversationMappingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "channel_type", nullable = false)
    private String channelType;

    @Column(name = "external_session_id")
    private String externalSessionId;

    @Column(name = "external_user_id")
    private String externalUserId;

    @Column(name = "chatwoot_conversation_id", nullable = false, unique = true)
    private Long chatwootConversationId;

    @Column(name = "internal_conversation_id", nullable = false)
    private Long internalConversationId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "status", nullable = false)
    private String status;
}

