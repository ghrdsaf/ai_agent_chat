package com.aiagentchat.backend.integration.extension.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.List;

public record ExtensionMessageDto(
        @NotBlank String externalMessageKey,
        @NotBlank String direction,
        @NotBlank String messageType,
        String text,
        List<String> imageUrls,
        List<String> linkUrls,
        OffsetDateTime occurredAt,
        String rawHtmlHash) {
}
