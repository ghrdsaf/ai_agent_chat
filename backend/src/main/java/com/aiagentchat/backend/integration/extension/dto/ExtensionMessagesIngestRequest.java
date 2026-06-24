package com.aiagentchat.backend.integration.extension.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ExtensionMessagesIngestRequest(
        @NotBlank String platformAccountId,
        @NotBlank String platform,
        @Valid ExtensionConversationDto conversation,
        @NotEmpty List<@Valid ExtensionMessageDto> messages) {
}
