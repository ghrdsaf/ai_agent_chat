package com.aiagentchat.backend.integration.extension.dto;

import java.util.List;

public record ExtensionMessagesIngestResponse(
        int accepted,
        int duplicates,
        List<ExtensionSuggestionDto> suggestions) {
}
