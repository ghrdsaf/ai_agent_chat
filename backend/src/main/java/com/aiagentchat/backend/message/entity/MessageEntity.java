package com.aiagentchat.backend.message.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "message")
public class MessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "channel_type", nullable = false)
    private String channelType;

    @Column(name = "sender_type", nullable = false)
    private String senderType;

    @Column(name = "message_type", nullable = false)
    private String messageType;

    @Column(name = "message_text", nullable = false, length = 4000)
    private String messageText;

    @Column(name = "message_time", nullable = false)
    private OffsetDateTime messageTime;

    @Column(name = "fingerprint", nullable = false, unique = true, length = 512)
    private String fingerprint;

    @Column(name = "process_status", nullable = false)
    private String processStatus;

    @Column(name = "risk_level", nullable = false)
    private String riskLevel;
}

