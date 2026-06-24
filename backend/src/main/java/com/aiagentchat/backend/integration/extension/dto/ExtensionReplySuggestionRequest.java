package com.aiagentchat.backend.integration.extension.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record ExtensionReplySuggestionRequest(
        @NotBlank String platform,
        ExtensionPlatformAccountHintDto accountHint,
        ExtensionConversationDto conversation,
        @Valid ExtensionMessageDto message) {
}
