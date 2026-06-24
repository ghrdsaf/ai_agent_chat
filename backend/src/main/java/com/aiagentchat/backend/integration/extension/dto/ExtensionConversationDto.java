package com.aiagentchat.backend.integration.extension.dto;

import jakarta.validation.constraints.NotBlank;

public record ExtensionConversationDto(
        @NotBlank String externalConversationKey,
        String buyerName,
        String title) {
}
