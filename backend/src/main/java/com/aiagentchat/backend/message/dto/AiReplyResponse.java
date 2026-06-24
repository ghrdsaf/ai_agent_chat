package com.aiagentchat.backend.message.dto;

import java.util.List;

public record AiReplyResponse(
        String replyText,
        String riskLevel,
        Double confidence,
        List<String> evidence,
        String action) {
}

