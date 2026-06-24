package com.aiagentchat.backend.integration.extension;

import com.aiagentchat.backend.integration.extension.dto.ExtensionMessageDto;
import com.aiagentchat.backend.integration.extension.dto.ExtensionMessagesIngestRequest;
import com.aiagentchat.backend.integration.extension.dto.ExtensionMessagesIngestResponse;
import com.aiagentchat.backend.integration.extension.dto.ExtensionPlatformAccountResolveRequest;
import com.aiagentchat.backend.integration.extension.dto.ExtensionPlatformAccountResolveResponse;
import com.aiagentchat.backend.integration.extension.dto.ExtensionReplySuggestionRequest;
import com.aiagentchat.backend.integration.extension.dto.ExtensionSuggestionDto;
import com.aiagentchat.backend.message.dto.AiReplyRequest;
import com.aiagentchat.backend.message.dto.AiReplyResponse;
import com.aiagentchat.backend.message.entity.ConversationMappingEntity;
import com.aiagentchat.backend.message.entity.MessageEntity;
import com.aiagentchat.backend.message.repository.ConversationMappingRepository;
import com.aiagentchat.backend.message.repository.MessageRepository;
import com.aiagentchat.backend.message.service.AiReplyService;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExtensionIngestService {

    private final ConversationMappingRepository conversationMappingRepository;
    private final MessageRepository messageRepository;
    private final AiReplyService aiReplyService;

    public ExtensionPlatformAccountResolveResponse resolvePlatformAccount(
            ExtensionPlatformAccountResolveRequest request) {
        String accountKey = buildAccountKey(request.platform(), request.shopName(), request.accountName());
        return new ExtensionPlatformAccountResolveResponse(accountKey, "active");
    }

    @Transactional
    public ExtensionMessagesIngestResponse ingestMessages(ExtensionMessagesIngestRequest request) {
        ConversationMappingEntity conversation = findOrCreateConversation(request);
        int accepted = 0;
        int duplicates = 0;
        List<ExtensionSuggestionDto> suggestions = new ArrayList<>();

        for (ExtensionMessageDto incoming : request.messages()) {
            String fingerprint = buildMessageFingerprint(request, incoming);
            if (messageRepository.existsByFingerprint(fingerprint)) {
                duplicates += 1;
                continue;
            }

            MessageEntity message = new MessageEntity();
            message.setConversationId(conversation.getInternalConversationId());
            message.setChannelType(request.platform());
            message.setSenderType(toSenderType(incoming.direction()));
            message.setMessageType(incoming.messageType());
            message.setMessageText(defaultString(incoming.text(), buildNonTextPlaceholder(incoming)));
            message.setMessageTime(incoming.occurredAt() != null ? incoming.occurredAt() : OffsetDateTime.now());
            message.setFingerprint(fingerprint);
            message.setProcessStatus("NEW");
            message.setRiskLevel("PENDING");
            message = messageRepository.save(message);
            accepted += 1;

            if ("customer".equals(message.getSenderType())) {
                suggestions.add(buildSuggestion(message, incoming.externalMessageKey()));
            }
        }

        return new ExtensionMessagesIngestResponse(accepted, duplicates, suggestions);
    }

    public ExtensionSuggestionDto suggestReply(ExtensionReplySuggestionRequest request) {
        ExtensionMessageDto message = request.message();
        try {
            AiReplyResponse response = aiReplyService.requestSuggestion(new AiReplyRequest(
                    null,
                    request.platform(),
                    null,
                    defaultString(message.text(), "")));
            return toSuggestion(message.externalMessageKey(), null, response);
        } catch (RuntimeException ex) {
            return fallbackSuggestion(message.externalMessageKey(), null, defaultString(message.text(), ""));
        }
    }

    private ConversationMappingEntity findOrCreateConversation(ExtensionMessagesIngestRequest request) {
        String externalSessionId = request.conversation().externalConversationKey();
        return conversationMappingRepository
                .findByChannelTypeAndExternalSessionId(request.platform(), externalSessionId)
                .orElseGet(() -> {
                    ConversationMappingEntity entity = new ConversationMappingEntity();
                    entity.setChannelType(request.platform());
                    entity.setExternalSessionId(externalSessionId);
                    entity.setExternalUserId(request.conversation().buyerName());
                    entity.setChatwootConversationId(stablePositiveId(request.platform() + ":" + externalSessionId));
                    entity.setInternalConversationId(stablePositiveId("internal:" + request.platform() + ":" + externalSessionId));
                    entity.setStatus("OPEN");
                    return conversationMappingRepository.save(entity);
                });
    }

    private ExtensionSuggestionDto buildSuggestion(MessageEntity message, String externalMessageKey) {
        try {
            AiReplyResponse response = aiReplyService.requestSuggestion(new AiReplyRequest(
                    message.getId(),
                    message.getChannelType(),
                    null,
                    message.getMessageText()));
            return toSuggestion(externalMessageKey, message.getId(), response);
        } catch (RuntimeException ex) {
            return fallbackSuggestion(externalMessageKey, message.getId(), message.getMessageText());
        }
    }

    private ExtensionSuggestionDto toSuggestion(String externalMessageKey, Long messageId, AiReplyResponse response) {
        String suggestionId = messageId != null ? "msg_" + messageId : "direct_" + externalMessageKey;
        return new ExtensionSuggestionDto(
                externalMessageKey,
                suggestionId,
                response.replyText(),
                response.riskLevel(),
                response.action());
    }

    private ExtensionSuggestionDto fallbackSuggestion(String externalMessageKey, Long messageId, String messageText) {
        String text = messageText != null && messageText.contains("发货")
                ? "亲，这款商品正常会尽快安排发出，具体时效以页面和订单信息为准哦。"
                : "亲，您好，我看到了您的问题，这边马上帮您确认一下。";
        return new ExtensionSuggestionDto(
                externalMessageKey,
                messageId != null ? "msg_" + messageId : "direct_" + externalMessageKey,
                text,
                "low",
                "human_confirm");
    }

    private String buildMessageFingerprint(ExtensionMessagesIngestRequest request, ExtensionMessageDto message) {
        return request.platform()
                + ":"
                + request.conversation().externalConversationKey()
                + ":"
                + message.externalMessageKey();
    }

    private String buildAccountKey(String platform, String shopName, String accountName) {
        return platform
                + ":"
                + defaultString(shopName, "unknown-shop")
                + ":"
                + defaultString(accountName, "default");
    }

    private String toSenderType(String direction) {
        if ("merchant".equalsIgnoreCase(direction)) {
            return "agent";
        }
        if ("system".equalsIgnoreCase(direction)) {
            return "system";
        }
        return "customer";
    }

    private String buildNonTextPlaceholder(ExtensionMessageDto message) {
        if (message.imageUrls() != null && !message.imageUrls().isEmpty()) {
            return "[图片消息]";
        }
        return "[" + defaultString(message.messageType(), "unknown") + "]";
    }

    private Long stablePositiveId(String value) {
        return (long) Math.abs(value.hashCode());
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
