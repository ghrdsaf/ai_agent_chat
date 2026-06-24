package com.aiagentchat.backend.integration.extension.dto;

public record ExtensionSuggestionDto(
        String messageKey,
        String suggestionId,
        String text,
        String riskLevel,
        String action) {
}
