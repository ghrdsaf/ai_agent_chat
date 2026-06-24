package com.aiagentchat.backend.message;

import com.aiagentchat.backend.integration.chatwoot.dto.ChatwootWebhookPayload;
import com.aiagentchat.backend.message.dto.AiReplyRequest;
import com.aiagentchat.backend.message.entity.ConversationMappingEntity;
import com.aiagentchat.backend.message.entity.MessageEntity;
import com.aiagentchat.backend.message.repository.ConversationMappingRepository;
import com.aiagentchat.backend.message.repository.MessageRepository;
import com.aiagentchat.backend.message.service.AiReplyService;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MessageIngestService {

    private final ConversationMappingRepository conversationMappingRepository;
    private final MessageRepository messageRepository;
    private final AiReplyService aiReplyService;

    @Transactional
    public void handleChatwootWebhook(
            ChatwootWebhookPayload payload,
            String signature,
            String timestamp) {
        if (!isMessageEvent(payload)) {
            return;
        }

        Long chatwootConversationId =
                payload.getConversation() != null ? payload.getConversation().getId() : null;
        if (chatwootConversationId == null) {
            return;
        }

        ConversationMappingEntity conversation = conversationMappingRepository
                .findByChatwootConversationId(chatwootConversationId)
                .orElseGet(() -> createConversationMapping(payload, chatwootConversationId));

        String fingerprint = buildFingerprint(payload, chatwootConversationId);
        if (messageRepository.existsByFingerprint(fingerprint)) {
            return;
        }

        MessageEntity message = new MessageEntity();
        message.setConversationId(conversation.getInternalConversationId());
        message.setChannelType(conversation.getChannelType());
        message.setSenderType(resolveSenderType(payload));
        message.setMessageType(defaultString(payload.getMessageType(), "text"));
        message.setMessageText(defaultString(payload.getContent(), ""));
        message.setMessageTime(payload.getCreatedAt() != null ? payload.getCreatedAt() : OffsetDateTime.now());
        message.setFingerprint(fingerprint);
        message.setProcessStatus("NEW");
        message.setRiskLevel("PENDING");
        message = messageRepository.save(message);

        if ("customer".equalsIgnoreCase(message.getSenderType())) {
            aiReplyService.requestSuggestion(new AiReplyRequest(
                    message.getId(),
                    conversation.getChannelType(),
                    conversation.getProductId(),
                    message.getMessageText()));
        }
    }

    private boolean isMessageEvent(ChatwootWebhookPayload payload) {
        String event = payload.getEvent();
        return event != null && event.toLowerCase().contains("message");
    }

    private ConversationMappingEntity createConversationMapping(
            ChatwootWebhookPayload payload,
            Long chatwootConversationId) {
        ConversationMappingEntity conversation = new ConversationMappingEntity();
        conversation.setChannelType("chatwoot");
        conversation.setExternalSessionId(
                payload.getConversation() != null
                        && payload.getConversation().getContactInbox() != null
                        ? payload.getConversation().getContactInbox().getSourceId()
                        : null);
        conversation.setExternalUserId(payload.getSender() != null ? payload.getSender().getIdentifier() : null);
        conversation.setChatwootConversationId(chatwootConversationId);
        conversation.setInternalConversationId(chatwootConversationId);
        conversation.setStatus("OPEN");
        return conversationMappingRepository.save(conversation);
    }

    private String resolveSenderType(ChatwootWebhookPayload payload) {
        String messageType = payload.getMessageType();
        if ("outgoing".equalsIgnoreCase(messageType)) {
            return "agent";
        }
        return "customer";
    }

    private String buildFingerprint(ChatwootWebhookPayload payload, Long conversationId) {
        return conversationId + ":" + payload.getId() + ":" + defaultString(payload.getContent(), "");
    }

    private String defaultString(String value, String fallback) {
        return value == null ? fallback : value;
    }
}

