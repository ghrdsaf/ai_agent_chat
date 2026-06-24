package com.aiagentchat.backend.message.dto;

public record AiReplyRequest(
        Long messageId,
        String channelType,
        Long productId,
        String messageText) {
}

